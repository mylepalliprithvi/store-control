package log;

import branch.Ref;
import commit.CommitModel;
import hash.HashContent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Log {

    public static void log() throws Exception {
        Path branchRefPath = Ref.getCurrentBranchRefPath();
        String branchRefPathString = String.valueOf(branchRefPath);
        String branchName = branchRefPathString.split("/")[3];
        System.out.println("Commits on branch: "+branchName+" in descending order");
        String latestCommit = Files.readString(branchRefPath).trim();
        CommitModel deserializedCommit = deserializeCommit(latestCommit);

        while(deserializedCommit!=null)
        {
            System.out.println("Commit: "+deserializedCommit.getCommitHash()+ " Message: "+deserializedCommit.getCommitMessage()+ " Author: "+deserializedCommit.getCommitAuthorName()+" <"+deserializedCommit.getCommitAuthorEmail()+"> "+" at: "+deserializedCommit.getCommitTimeStamp());
            if(deserializedCommit.getParentCommitHash().isEmpty())
            {
                break;
            }
            deserializedCommit = deserializeCommit(deserializedCommit.getParentCommitHash());
        }
    }


    public static CommitModel deserializeCommit(String commit) throws Exception {
        String deHash = HashContent.readObject(commit);
        //tree <treehash>\n parent <parenthash>\n author <author> <authoremail>\n timestamp <timestamp>\n \n<commitmessage>
        String[] splitDeHash = deHash.split("\n");
        String treeContent = splitDeHash[0];
        String parentContent="";
        String timestamp;
        String commitMessage;
        boolean parentExists = false;
        if(splitDeHash[1].contains("parent "))
        {
            parentContent = splitDeHash[1];
            parentExists = true;
        }
        String authorContent;
        if(parentExists)
        {
            authorContent = splitDeHash[2];
            timestamp = splitDeHash[3];
            commitMessage = splitDeHash[5];

        }
        else {
            authorContent = splitDeHash[1];
            timestamp = splitDeHash[2];
            commitMessage = splitDeHash[4];
        }
        String treeHash = treeContent.substring(5);
        String actualAuthorDetails = authorContent.substring(7);
        String[] authorDetails = actualAuthorDetails.split(" ");
        String authorName = authorDetails[0]+" "+authorDetails[1];
        String authorEmail = authorDetails[2];
        String parentCommitHash = parentExists?parentContent.substring(7):"";
        CommitModel deserializedCommit = new CommitModel(authorName,authorEmail,parentCommitHash,timestamp,commitMessage,treeHash);
        deserializedCommit.setCommitHash(commit);
        return deserializedCommit;
    }
}
