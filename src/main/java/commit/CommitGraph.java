package commit;

import common.Constants;
import log.Log;
import tree.Tree;
import tree.TreeEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CommitGraph {

    // True if ancestorCommitHash is reachable from descendantCommitHash by walking
    // parent/merge-parent pointers. An empty/null ancestor is trivially reachable
    // (no commits yet on that side, e.g. a brand new branch).
    public static boolean isAncestor(String ancestorCommitHash, String descendantCommitHash) throws Exception {
        if(ancestorCommitHash == null || ancestorCommitHash.isEmpty())
        {
            return true;
        }
        Deque<String> toVisit = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        toVisit.push(descendantCommitHash);
        while(!toVisit.isEmpty())
        {
            String current = toVisit.pop();
            if(current == null || current.isEmpty() || !visited.add(current))
            {
                continue;
            }
            if(current.equals(ancestorCommitHash))
            {
                return true;
            }
            CommitModel commitModel = Log.deserializeCommit(current);
            toVisit.push(commitModel.getParentCommitHash());
            if(commitModel.getMergeParentCommitHash() != null && !commitModel.getMergeParentCommitHash().isEmpty())
            {
                toVisit.push(commitModel.getMergeParentCommitHash());
            }
        }
        return false;
    }

    // Walks descendantCommitHash's ancestry back to (but not including)
    // ancestorCommitHash, collecting every commit/tree/blob object reachable
    // along the way. Used by push (send what the remote doesn't have yet) and
    // pull (send what the client doesn't have yet) - same walk either way,
    // just from whichever side is doing the collecting.
    public static Map<String,byte[]> collectObjects(String descendantCommitHash, String ancestorCommitHash) throws Exception {
        Map<String,byte[]> objects = new HashMap<>();
        Deque<String> toVisit = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        toVisit.push(descendantCommitHash);
        while(!toVisit.isEmpty())
        {
            String current = toVisit.pop();
            if(current == null || current.isEmpty() || current.equals(ancestorCommitHash) || !visited.add(current))
            {
                continue;
            }
            addObject(objects,current);
            String treeHash = Commit.getTreeHashFromCommit(current);
            addObject(objects,treeHash);
            for(TreeEntry entry : Tree.readTree(treeHash,""))
            {
                addObject(objects,entry.getFileContentHash());
            }
            CommitModel commitModel = Log.deserializeCommit(current);
            toVisit.push(commitModel.getParentCommitHash());
            if(commitModel.getMergeParentCommitHash() != null && !commitModel.getMergeParentCommitHash().isEmpty())
            {
                toVisit.push(commitModel.getMergeParentCommitHash());
            }
        }
        return objects;
    }

    private static void addObject(Map<String,byte[]> objects, String hash) throws IOException {
        if(objects.containsKey(hash))
        {
            return;
        }
        Path objPath = Path.of(Constants.root+"/"+Constants.objects,hash.substring(0,2),hash.substring(2));
        objects.put(hash,Files.readAllBytes(objPath));
    }
}
