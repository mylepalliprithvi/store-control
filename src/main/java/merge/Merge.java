package merge;

import branch.Ref;
import commit.Commit;
import commit.CommitGraph;
import commit.CommitModel;
import common.Constants;
import common.DateTimeUtil;
import hash.HashContent;
import org.json.JSONArray;
import org.json.JSONObject;
import tree.Tree;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/*
    store's merge never produces conflict markers: whenever both sides touch the
    same file differently, both versions are kept on disk (one under the
    original path, one suffixed ".local") instead of asking the user to
    reconcile them by hand. Only byte-identical content collapses to one copy.
*/
public class Merge {

    public static void merge(String branchToMerge) throws Exception {
        Path currentBranchRefPath = Ref.getCurrentBranchRefPath();
        String currentBranchName = currentBranchRefPath.getFileName().toString();

        if(branchToMerge.equals(currentBranchName))
        {
            System.out.println("Already on branch '"+branchToMerge+"', nothing to merge");
            return;
        }

        Path headsPath = Path.of(Constants.heads);
        Path targetBranchRefPath = Path.of(headsPath+"/"+branchToMerge);
        if(Files.notExists(targetBranchRefPath))
        {
            System.out.println("Branch '"+branchToMerge+"' does not exist");
            return;
        }

        String currentHead = Files.exists(currentBranchRefPath) ? Files.readString(currentBranchRefPath).trim() : "";
        String targetHead = Files.readString(targetBranchRefPath).trim();

        if(targetHead.isEmpty())
        {
            System.out.println("Branch '"+branchToMerge+"' has no commits yet, nothing to merge");
            return;
        }

        if(currentHead.equals(targetHead))
        {
            System.out.println("Already up to date");
            return;
        }

        Map<String,String> currentBlobPaths = getTreeBlobPaths(currentHead);

        Map<Path,String> stashed = stashDirtyFiles(currentBlobPaths);
        if(!stashed.isEmpty())
        {
            System.out.println("Stashed "+stashed.size()+" uncommitted change(s) on '"+currentBranchName+"'");
        }

        if(CommitGraph.isAncestor(currentHead,targetHead))
        {
            System.out.println("Fast-forwarding '"+currentBranchName+"' to '"+branchToMerge+"'");
            Map<String,String> targetBlobPaths = getTreeBlobPaths(targetHead);
            Tree.materializeBlobPaths(targetBlobPaths,currentBlobPaths,true);
            Files.writeString(currentBranchRefPath,targetHead);
        }
        else
        {
            System.out.println("Merging '"+branchToMerge+"' into '"+currentBranchName+"'");
            Map<String,String> targetBlobPaths = getTreeBlobPaths(targetHead);
            Map<String,String> mergedBlobPaths = reconcile(currentBlobPaths,targetBlobPaths);
            Tree.materializeBlobPaths(mergedBlobPaths,currentBlobPaths,false);

            String mergeTreeHash = buildTreeFromPaths(mergedBlobPaths);
            String mergeCommitHash = createMergeCommit(mergeTreeHash,currentHead,targetHead,
                    "Merge branch '"+branchToMerge+"' into "+currentBranchName);
            Files.writeString(currentBranchRefPath,mergeCommitHash);
            System.out.println("Created merge commit: "+mergeCommitHash);
        }

        if(!stashed.isEmpty())
        {
            System.out.println("Reapplying stashed changes");
            for(Map.Entry<Path,String> entry : stashed.entrySet())
            {
                applyIncoming(entry.getKey(),entry.getValue());
            }
        }

        System.out.println("Merge complete. Run 'store status' to review, then 'store add'/'store commit' any *.local files.");
    }

    // Reads every path in the index, and any whose on-disk content no longer
    // matches the current branch's HEAD tree is "dirty": its content is saved
    // aside and the working copy is reverted to the committed version, so the
    // merge below starts from a clean tree (mirrors git stash).
    private static Map<Path,String> stashDirtyFiles(Map<String,String> headBlobPaths) throws Exception {
        Map<Path,String> stashed = new HashMap<>();
        Path indexPath = Path.of(Constants.root+"/"+Constants.indexFile);
        if(Files.notExists(indexPath))
        {
            return stashed;
        }
        JSONArray indexArray = new JSONArray(new String(Files.readAllBytes(indexPath)));
        for(Object object : indexArray)
        {
            JSONObject entry = (JSONObject) object;
            String pathString = entry.getString("path");
            Path path = Path.of(pathString);
            if(Files.notExists(path))
            {
                continue;
            }
            String onDiskContent = Files.readString(path);
            String onDiskHash = HashContent.computeHash(onDiskContent.getBytes(),"blob");
            String headHash = headBlobPaths.get(pathString);
            if(headHash == null)
            {
                // never committed on this branch - stash it and clear it away entirely
                stashed.put(path,onDiskContent);
                Files.deleteIfExists(path);
            }
            else if(!headHash.equals(onDiskHash))
            {
                stashed.put(path,onDiskContent);
                Files.writeString(path,HashContent.readObject(headHash));
            }
        }
        return stashed;
    }

    // Union of two branches' tracked files. Paths on only one side are kept
    // as-is (git wouldn't call that a conflict either). Paths on both sides
    // with identical content collapse to one. Paths on both sides that
    // differ keep the target branch's content at the original path and the
    // current branch's content under a ".local" suffixed path - this is the
    // step that replaces conflict markers entirely.
    private static Map<String,String> reconcile(Map<String,String> currentBlobPaths, Map<String,String> targetBlobPaths) {
        Map<String,String> merged = new HashMap<>(currentBlobPaths);
        for(Map.Entry<String,String> entry : targetBlobPaths.entrySet())
        {
            String path = entry.getKey();
            String targetHash = entry.getValue();
            String existingHash = merged.get(path);
            if(existingHash == null)
            {
                merged.put(path,targetHash);
            }
            else if(!existingHash.equals(targetHash))
            {
                merged.put(suffixedPathString(path),existingHash);
                merged.put(path,targetHash);
            }
        }
        return merged;
    }

    // Writes incomingContent to targetPath, same no-conflict rule as reconcile():
    // identical content is a no-op, different content preserves whatever is
    // already there under a ".local" suffix before the incoming content lands.
    private static void applyIncoming(Path targetPath, String incomingContent) throws Exception {
        if(Files.exists(targetPath))
        {
            String existingContent = Files.readString(targetPath);
            if(existingContent.equals(incomingContent))
            {
                return;
            }
            Files.writeString(suffixedPath(targetPath),existingContent);
        }
        Path parent = targetPath.getParent();
        if(parent != null)
        {
            Files.createDirectories(parent);
        }
        Files.writeString(targetPath,incomingContent);
    }

    private static Path suffixedPath(Path path) {
        return Path.of(suffixedPathString(path.toString()));
    }

    private static String suffixedPathString(String pathString) {
        int slash = Math.max(pathString.lastIndexOf('/'),pathString.lastIndexOf('\\'));
        String dir = slash >= 0 ? pathString.substring(0,slash+1) : "";
        String fileName = slash >= 0 ? pathString.substring(slash+1) : pathString;
        int dot = fileName.lastIndexOf('.');
        String suffixedName = dot > 0 ? fileName.substring(0,dot)+".local"+fileName.substring(dot) : fileName+".local";
        return dir+suffixedName;
    }

    private static Map<String,String> getTreeBlobPaths(String commitHash) throws Exception {
        return Commit.getBlobPathsForCommit(commitHash);
    }

    private static String buildTreeFromPaths(Map<String,String> blobPaths) throws Exception {
        JSONArray indexArray = new JSONArray();
        for(Map.Entry<String,String> entry : blobPaths.entrySet())
        {
            JSONObject object = new JSONObject();
            object.put("path",entry.getKey());
            object.put("hash",entry.getValue());
            indexArray.put(object);
        }
        return Tree.writeTreeFromIndex(indexArray);
    }

    private static String createMergeCommit(String treeHash, String parentHash, String mergeParentHash, String message) throws Exception {
        Path configPath = Path.of(Constants.root+"/"+Constants.configFile);
        JSONObject configJSON = new JSONObject(new String(Files.readAllBytes(configPath)));

        CommitModel commitModel = new CommitModel(configJSON.getString("authorName"),configJSON.getString("authorEmail"),
                parentHash,DateTimeUtil.getCurrentTimestamp(),message,treeHash);
        commitModel.setMergeParentCommitHash(mergeParentHash);

        byte[] serialized = Commit.serialize(commitModel);
        String commitHash = HashContent.hashObject(serialized,"commit");
        commitModel.setCommitHash(commitHash);
        return commitHash;
    }
}
