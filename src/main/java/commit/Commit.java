package commit;

import branch.Ref;
import commit.CommitModel;
import common.Constants;
import common.DateTimeUtil;
import hash.HashContent;
import org.json.JSONArray;
import org.json.JSONObject;
import tree.Tree;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

public class Commit {

    public static void commit(String commitMessage) throws IOException, NoSuchAlgorithmException {
        Path indexPath = Path.of(Constants.root+"/"+Constants.indexFile);
        Path configPath = Path.of(Constants.root+"/"+Constants.configFile);

        String indexContents = new String(Files.readAllBytes(indexPath));
        JSONArray indexArray = new JSONArray(indexContents);
        String configContents = new String(Files.readAllBytes(configPath));
        JSONObject configJSON = new JSONObject(configContents);
        String rootTreeHash = Tree.writeTreeFromIndex(indexArray);
        String parentCommitHash = getParentCommitHash();

        CommitModel commit = new CommitModel(configJSON.getString("authorName"),configJSON.getString("authorEmail"),parentCommitHash, DateTimeUtil.getCurrentTimestamp(),commitMessage,rootTreeHash);
        byte[] serializedCommit = serialize(commit);
        String hashSerializedCommit = HashContent.hashObject(serializedCommit,"commit");
        commit.setCommitHash(hashSerializedCommit);

        Path branchRefPath = Ref.getCurrentBranchRefPath();
        Files.createDirectories(branchRefPath.getParent());
        Files.writeString(branchRefPath, hashSerializedCommit);

        System.out.println("Hash of commit: "+hashSerializedCommit);
    }


    // The current branch ref file's content, if it exists, is the branch's
    // current tip commit hash -> the new commit's parent. No ref file yet
    // means this is the first commit on the branch, so there's no parent.
    private static String getParentCommitHash() throws IOException {
        Path branchRefPath = Ref.getCurrentBranchRefPath();
        if (!Files.exists(branchRefPath)) {
            return "";
        }
        return Files.readString(branchRefPath).trim();
    }

    public static byte[] serialize(CommitModel commit)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("tree ").append(commit.getTreeHash()).append("\n");
        if(commit.getParentCommitHash()!=null && !commit.getParentCommitHash().isEmpty())
        {
            sb.append("parent ").append(commit.getParentCommitHash()).append("\n");
        }
        sb.append("author ").append(commit.getCommitAuthorName()).append(" ").append(commit.getCommitAuthorEmail()).append("\n");
        sb.append("timestamp ").append(commit.getCommitTimeStamp()).append("\n");
        sb.append("\n");
        sb.append(commit.getCommitMessage());
        return sb.toString().getBytes();
    }
}
