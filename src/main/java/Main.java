import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.zip.Deflater;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Inflater;

public class Main {
    public static void main(String[] args) throws Exception {
//        String c = "abcd";
//        byte[] input = c.getBytes(StandardCharsets.UTF_8);
//        String res = hashObject(input,"blob");
        String hash = "85df50785d62d3b05ab03d9cbf7e4a0b49449730";
        String content = readObject(hash);
        System.out.println(content);
    }

    /*
        @params:
        byte array containing contents needed to be hashed
        string type
        @returns:
        string hash id
     */
    public static String hashObject(byte[] content, String type) throws NoSuchAlgorithmException, IOException {
        int contentSize = content.length;
        String headerMessage = type+" "+String.valueOf(contentSize)+"\0";
        byte[] headerBytes = headerMessage.getBytes(StandardCharsets.UTF_8);

        byte[] store  = new byte[headerBytes.length + contentSize];
        System.arraycopy(headerBytes,0,store,0,headerBytes.length);
        System.arraycopy(content,0,store,headerBytes.length,contentSize);


        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(store);
        StringBuilder hex = new StringBuilder();
        for(byte b : digest)
        {
            hex.append(String.format("%02x",b));
        }
        String hashMessage = hex.toString();
        System.out.println("Hashed Message: "+hashMessage);

        Deflater deflater = new Deflater();
        deflater.setInput(store);
        deflater.finish();

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while(!deflater.finished())
        {
            int n = deflater.deflate(buffer);
            compressed.write(buffer,0,n);
        }
        Path dir = Paths.get(".mygit","objects",hashMessage.substring(0,2));
        Files.createDirectories(dir);
        Files.write(dir.resolve(hashMessage.substring(2)),compressed.toByteArray());
        return hashMessage;
    }


    /*
    @params:
    hashMessage : string
    @returns:
    content : string
     */

    public static String readObject(String hashMessage) throws Exception
    {
        String initPath = hashMessage.substring(0,2);
        String nextPath = hashMessage.substring(2);

        Path path = Paths.get(".mygit","objects",initPath,nextPath);
        File file = new File(path.toUri());
        if(!file.exists())
        {
            System.out.println("File does not exist!!");
        }
        byte[] contentBytes = Files.readAllBytes(path);
        byte[] buffer = new byte[1024];
        Inflater inflater = new Inflater();
        inflater.setInput(contentBytes);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        while(!inflater.finished())
        {
            int n = inflater.inflate(buffer);
            outputStream.write(buffer,0,n);
        }
        inflater.end();
        byte[] output = outputStream.toByteArray();
        String outputMessage = new String(output);
        String[] splitArray = outputMessage.split("\0");
        return splitArray[1];
    }

}
