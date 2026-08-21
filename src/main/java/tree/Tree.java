package tree;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import hash.HashContent;


public class Tree {
    /*
    file type file name hash
     */

    public static String writeTree(File directory) throws IOException, NoSuchAlgorithmException {
        List<String> listOfEntries = new ArrayList<>();
        /*
            get list of all files in the directory
            for each file, perform hash and store in .store
            in a directory, again there might be directory, so every folder new tree again

         */
        File[] listOfFiles = directory.listFiles();
        for(File file : listOfFiles)
        {
            String fileType;
            String hashOfContent;
            String fileMode;
            if(file.isDirectory())
            {
                hashOfContent = writeTree(file);
                fileType = "tree";
                fileMode = "directory";
            }
            else
            {
                byte[] fileContent = Files.readAllBytes(file.toPath());
                fileType = "blob";
                fileMode = "file";
                hashOfContent = HashContent.hashObject(fileContent,fileType);
            }
            //TreeEntry entry = new TreeEntry(fileMode,fileType,hashOfContent,file.getName());
            String entry = fileMode+","+fileType+","+hashOfContent+","+file.getName();
            listOfEntries.add(entry);
        }
        byte[] serializedListOfTreeEntries = String.join("\n",listOfEntries).getBytes();
        return HashContent.hashObject(serializedListOfTreeEntries,"tree");
    }

    /*
        @param String hashSerializedListOfTreeEntries
        returns
        @void - prints out structure
     */
    public static void readTree(String hashSerializedListOfTreeEntries) throws Exception {
        String dehash = HashContent.readObject(hashSerializedListOfTreeEntries);
        if(!dehash.isEmpty())
        {
            String[] splitStrings = dehash.split("\n");

            for(String entry : splitStrings)
            {
                String[] decodedEntry = entry.split(",",4);
                String fileMode = decodedEntry[0];
                String fileType = decodedEntry[1];
                String hash = decodedEntry[2];
                String fileName = decodedEntry[3];
                if(fileType.equals("tree"))
                {
                    readTree(hash);
                }
                System.out.println("Mode: "+fileMode+ " Type: "+fileType+" Hash: "+hash+" Name: "+fileName);
            }
        }


    }
}
