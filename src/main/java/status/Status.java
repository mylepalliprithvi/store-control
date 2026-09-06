package status;

import branch.Ref;
import commit.Commit;
import common.Constants;
import file.FileUtil;
import hash.HashContent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Status {

    public static void status() throws Exception {
        Map<String,String> headBlobPaths = getHeadBlobPaths();
        Map<String,String> indexBlobPaths = getIndexBlobPaths();
        Map<String,String> workingFileHashes = getWorkingFileHashes();

        List<String> untracked = new ArrayList<>();
        List<String> modifiedNotStaged = new ArrayList<>();
        List<String> stagedForCommit = new ArrayList<>();
        List<String> deleted = new ArrayList<>();

        for(Map.Entry<String,String> entry : workingFileHashes.entrySet())
        {
            String path = entry.getKey();
            String hash = entry.getValue();
            if(!indexBlobPaths.containsKey(path))
            {
                untracked.add(path);
            }
            else if(!indexBlobPaths.get(path).equals(hash))
            {
                modifiedNotStaged.add(path);
            }
        }

        for(Map.Entry<String,String> entry : indexBlobPaths.entrySet())
        {
            String path = entry.getKey();
            String indexHash = entry.getValue();
            if(!workingFileHashes.containsKey(path))
            {
                deleted.add(path);
            }
            String headHash = headBlobPaths.get(path);
            if(headHash == null || !headHash.equals(indexHash))
            {
                stagedForCommit.add(path);
            }
        }

        System.out.println("On branch: "+getCurrentBranchName());

        printSection("Staged for commit",stagedForCommit);
        printSection("Modified, not staged",modifiedNotStaged);
        printSection("Deleted, not staged",deleted);
        printSection("Untracked files",untracked);

        if(stagedForCommit.isEmpty() && modifiedNotStaged.isEmpty() && deleted.isEmpty() && untracked.isEmpty())
        {
            System.out.println("Nothing to commit, working tree clean");
        }
    }

    private static void printSection(String title, List<String> paths) {
        if(paths.isEmpty())
        {
            return;
        }
        System.out.println(title+":");
        for(String path : paths)
        {
            System.out.println("  "+path);
        }
    }

    private static String getCurrentBranchName() throws Exception {
        return Ref.getCurrentBranchRefPath().getFileName().toString();
    }

    private static Map<String,String> getHeadBlobPaths() throws Exception {
        Path branchRefPath = Ref.getCurrentBranchRefPath();
        if(Files.notExists(branchRefPath))
        {
            return new HashMap<>();
        }
        String headHash = Files.readString(branchRefPath).trim();
        return Commit.getBlobPathsForCommit(headHash);
    }

    private static Map<String,String> getIndexBlobPaths() throws Exception {
        Map<String,String> map = new HashMap<>();
        Path indexPath = Path.of(Constants.root+"/"+Constants.indexFile);
        if(Files.notExists(indexPath))
        {
            return map;
        }
        String indexContents = new String(Files.readAllBytes(indexPath));
        JSONArray indexArray = new JSONArray(indexContents);
        for(Object object : indexArray)
        {
            JSONObject entry = (JSONObject) object;
            map.put(entry.getString("path"),entry.getString("hash"));
        }
        return map;
    }

    private static Map<String,String> getWorkingFileHashes() throws Exception {
        Map<String,String> map = new HashMap<>();
        for(Path path : FileUtil.collectTrackableFiles(new File(".")))
        {
            byte[] content = Files.readAllBytes(path);
            String hash = HashContent.computeHash(content,"blob");
            map.put(path.toString(),hash);
        }
        return map;
    }
}
