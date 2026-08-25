package tree;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import hash.HashContent;
import org.json.JSONArray;
import org.json.JSONObject;


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
        @param JSONArray of index entries, each {path, type, hash}
        returns
        @String - hash of the root tree object

        The index is flat ("src/foo/bar.txt" -> blobHash), but a tree is
        nested. This regroups the flat paths into a directory hierarchy in
        memory first, then hashes it bottom-up (leaves/blobs first, so their
        hashes are known before the parent directory's tree entry line is
        built).
     */
    public static String writeTreeFromIndex(JSONArray indexArray) throws IOException, NoSuchAlgorithmException {
        // TreeMap keeps entries sorted by name at every level, so the same
        // index always produces the same serialized tree -> same hash.
        Map<String, Object> root = new TreeMap<>();

        for (Object object : indexArray) {
            JSONObject entry = (JSONObject) object;
            String path = entry.getString("path");
            String blobHash = entry.getString("hash");

            String[] segments = path.split("[/\\\\]");
            Map<String, Object> current = root;
            // walk/create every directory segment except the last (the file name)
            for (int i = 0; i < segments.length - 1; i++) {
                current = (Map<String, Object>) current.computeIfAbsent(
                        segments[i], k -> new TreeMap<String, Object>());
            }
            current.put(segments[segments.length - 1], blobHash);
        }

        return hashTreeMap(root);
    }

    // A node's values are either a String (blob hash, i.e. a file) or
    // another Map (a subdirectory, i.e. needs its own tree object first).
    private static String hashTreeMap(Map<String, Object> node) throws IOException, NoSuchAlgorithmException {
        List<String> entries = new ArrayList<>();
        for (Map.Entry<String, Object> child : node.entrySet()) {
            String name = child.getKey();
            Object value = child.getValue();

            String fileMode;
            String fileType;
            String hash;
            if (value instanceof Map) {
                fileMode = "directory";
                fileType = "tree";
                hash = hashTreeMap((Map<String, Object>) value); // recurse first, need its hash
            } else {
                fileMode = "file";
                fileType = "blob";
                hash = (String) value;
            }
            entries.add(fileMode + "," + fileType + "," + hash + "," + name);
        }

        byte[] serializedListOfTreeEntries = String.join("\n", entries).getBytes();
        return HashContent.hashObject(serializedListOfTreeEntries, "tree");
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
