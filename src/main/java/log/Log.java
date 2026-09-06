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
        //tree <treehash>\n [parent <parenthash>\n] [merge-parent <parenthash>\n] author <author> <authoremail>\n timestamp <timestamp>\n \n<commitmessage>
        String[] lines = deHash.split("\n",-1);
        String treeHash = lines[0].substring(5);
        String parentCommitHash = "";
        String mergeParentCommitHash = "";
        int i = 1;
        if(lines[i].startsWith("parent "))
        {
            parentCommitHash = lines[i].substring(7);
            i++;
        }
        if(lines[i].startsWith("merge-parent "))
        {
            mergeParentCommitHash = lines[i].substring(13);
            i++;
        }
        String authorContent = lines[i++];
        String timestamp = lines[i++];
        i++; // blank line separating header from message
        String commitMessage = String.join("\n",java.util.Arrays.copyOfRange(lines,i,lines.length));

        String actualAuthorDetails = authorContent.substring(7);
        String[] authorDetails = actualAuthorDetails.split(" ");
        String authorName = authorDetails[0]+" "+authorDetails[1];
        String authorEmail = authorDetails[2];
        CommitModel deserializedCommit = new CommitModel(authorName,authorEmail,parentCommitHash,timestamp,commitMessage,treeHash);
        deserializedCommit.setCommitHash(commit);
        deserializedCommit.setMergeParentCommitHash(mergeParentCommitHash);
        return deserializedCommit;
    }
}
