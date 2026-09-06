package add;

import common.Constants;
import hash.HashContent;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import index.IndexModel;

public class Add {

    private static Set<String> ignoredDirectories;
    private static Set<String> ignoredExtensions;

    /*
        @params: Path of file to be added
        @returns : void
        @performs: adding the file changes to index
     */
    public static void addFile(Path path) throws IOException, NoSuchAlgorithmException {
        if(ignoreFile(path))
        {
            return;
        }

        Path indexPath = Path.of(Constants.root+"/"+Constants.indexFile);
        String indexContents = new String(Files.readAllBytes(indexPath));
        JSONArray indexArray = new JSONArray(indexContents);
        String pathString = path.toString();
        String fileContent = new String(Files.readAllBytes(path));
        byte[] fileContentToBeAdded = fileContent.getBytes();
        String hashOfContentToAdd = HashContent.hashObject(fileContentToBeAdded,"blob");
        IndexModel indexToAdd = new IndexModel(pathString,"blob", hashOfContentToAdd);
        JSONObject indexJson = new JSONObject(indexToAdd);
        boolean addIndex = true;
        for(Object object : indexArray)
        {
            JSONObject json = (JSONObject) object;
            String pathOfJsonObject = json.getString("path");
            if(pathOfJsonObject.equals(pathString))
            {
                addIndex = false;
                json.put("hash",hashOfContentToAdd);
                break;
            }
        }
        if(addIndex)
        {
            indexArray.put(indexJson);
        }
        Files.writeString(indexPath,indexArray.toString());
    }

    public static boolean ignoreFile(Path path) throws IOException {
        loadIgnorePatternsIfNeeded();
        if(ignoredDirectories.isEmpty() && ignoredExtensions.isEmpty())
        {
            return false;
        }

        String pathAsString = path.toString();
        String normalizedPath = pathAsString.startsWith("./") ? pathAsString.substring(2) : pathAsString;

        for(String segment : normalizedPath.split("/"))
        {
            if(ignoredDirectories.contains(segment))
            {
                return true;
            }
        }

        for(String extension : ignoredExtensions)
        {
            if(pathAsString.endsWith(extension))
            {
                return true;
            }
        }

        return false;
    }

    // Cached for the life of the process - store add . calls this once per file
    // in the tree, and re-reading/re-parsing .storeignore that many times would
    // be wasted work.
    private static void loadIgnorePatternsIfNeeded() throws IOException {
        if(ignoredDirectories != null)
        {
            return;
        }
        ignoredDirectories = new HashSet<>();
        ignoredExtensions = new HashSet<>();

        Path storeIgnorePath = Path.of(Constants.storeIgnorePath);
        if(Files.notExists(storeIgnorePath))
        {
            return;
        }

        String storeIgnoreContent = Files.readString(storeIgnorePath);
        for(String line : storeIgnoreContent.split("\n"))
        {
            String content = line.strip();
            if(content.isEmpty() || content.startsWith("#"))
            {
                continue;
            }
            //ends with / indicates directory to skip
            if(content.endsWith("/"))
            {
                ignoredDirectories.add(content.substring(0,content.length()-1));
            }
            //contents ends with .{}* means end with .{} eg: .log .local .debug
            else if(content.startsWith(".") && content.endsWith("*"))
            {
                ignoredExtensions.add(content.substring(0,content.length()-1));
            }
        }
    }

}
