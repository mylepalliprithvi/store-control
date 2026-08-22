package add;

import common.Constants;
import hash.HashContent;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import common.JSONUtil;
import index.IndexModel;
public class Add {
    /*
        @params: Path of file to be added
        @returns : void
        @performs: adding the file changes to index
     */
    public static void addFile(Path path) throws IOException, NoSuchAlgorithmException {
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
}
