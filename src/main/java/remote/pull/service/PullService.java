package remote.pull.service;
import commit.Commit;
import common.Constants;
import hash.HashContent;
import log.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import remote.pull.model.PullResponseModel;
import tree.Tree;
import tree.TreeEntry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class PullService {
    private static final Logger LOGGER = LogManager.getLogger(PullService.class);

    public PullResponseModel pullCommits(String branchName) throws Exception {
        Path remoteBranchPath = Path.of(Constants.remoteOriginPath+"/"+branchName);
        if(Files.notExists(remoteBranchPath))
        {
            LOGGER.error("Pull service failed: Branch: {} does not have an upstream remote branch",branchName);
            PullResponseModel errorResponseModel = new PullResponseModel();
            errorResponseModel.setSuccess(false);
            errorResponseModel.setMessage("Rejected. Branch does not exist in remote");
            return errorResponseModel;
        }

        String remoteLatestCommitHash = Files.readString(remoteBranchPath);

        Path localBranchPath = Path.of(Constants.heads+"/"+branchName);
        if(Files.notExists(localBranchPath))
        {
            LOGGER.error("Pull service failed: Branch: {} does not exist",branchName);
            PullResponseModel errorResponseModel = new PullResponseModel();
            errorResponseModel.setSuccess(false);
            errorResponseModel.setMessage("Rejected. Branch does not exist");
            return errorResponseModel;
        }

        String latestLocalCommit = Files.readString(localBranchPath);

        if(remoteLatestCommitHash.equals(latestLocalCommit))
        {
            PullResponseModel responseModel = new PullResponseModel();
            responseModel.setMessage("Success. Pull skipped as up to date");
            responseModel.setSuccess(true);
            responseModel.setCommitHash(remoteLatestCommitHash);
            return responseModel;
        }
        else
        {
            //loop every commit not present in local and write files to disk accordingly
            if(!isAncestor(latestLocalCommit,remoteLatestCommitHash))
            {
                LOGGER.error("Pull rejected. Local commit: {} diverged from Remote commit: {}. Please merge!",latestLocalCommit,remoteLatestCommitHash);
                PullResponseModel errorResponseModel = new PullResponseModel();
                errorResponseModel.setSuccess(false);
                errorResponseModel.setMessage("Rejected. Merging required as branches have diverged");
                return errorResponseModel;
            }

            //If the two commits are ancestors then, check for new files in remote commit tree and to local tree
            Set<TreeEntry> remoteTree = Tree.readTree(Commit.getTreeHashFromCommit(remoteLatestCommitHash),"");
            Set<TreeEntry> localTree = Tree.readTree(Commit.getTreeHashFromCommit(latestLocalCommit),"");

            Map<String,TreeEntry> remoteTreeMap = new HashMap<>();
            for(TreeEntry entry : remoteTree)
            {
                remoteTreeMap.put(entry.getFilePath().toString(),entry);
            }

            Map<String,TreeEntry> localTreeMap = new HashMap<>();
            for(TreeEntry entry : localTree)
            {
                localTreeMap.put(entry.getFilePath().toString(),entry);
            }

            //now compare the two maps to check which does not exist

            for(Map.Entry<String,TreeEntry> entry : remoteTreeMap.entrySet())
            {
                String filePath = entry.getKey();
                TreeEntry remoteTreeEntry = entry.getValue();
                if(remoteTreeEntry.getFileType().equals("tree"))
                {
                    continue;
                }
                TreeEntry localTreeEntry = localTreeMap.get(filePath);
                if(localTreeEntry==null || !localTreeEntry.getFileContentHash().equals(remoteTreeEntry.getFileContentHash()))
                {
                    Path remoteEntryParentPath = remoteTreeEntry.getFilePath().getParent();
                    if(remoteEntryParentPath!=null)
                    {
                        Files.createDirectories(remoteEntryParentPath);
                    }
                    Files.writeString(remoteTreeEntry.getFilePath(), HashContent.readObject(remoteTreeEntry.getFileContentHash()));
                }
            }
            //delete files in local branch if recent commits in remote no longer contain them
            for(Map.Entry<String,TreeEntry> entry : localTreeMap.entrySet())
            {
                TreeEntry localEntry = entry.getValue();
                if(localEntry.getFileType().equals("blob") && !remoteTreeMap.containsKey(entry.getKey()))
                {
                    Files.deleteIfExists(localEntry.getFilePath());
                }
            }
            //update latest remote commit hash to local commit hash in .store/refs/heads/<branch>
            Files.writeString(localBranchPath,remoteLatestCommitHash);
            //return PullResponse model
            PullResponseModel successResponseModel = new PullResponseModel();
            successResponseModel.setMessage("Success. Latest commits received");
            successResponseModel.setCommitHash(remoteLatestCommitHash);
            successResponseModel.setSuccess(true);
            LOGGER.info("Successfully updated branch: {} with remote",branchName);
            return successResponseModel;
        }
    }

    private boolean isAncestor(String ancestorCommitHash, String descendantCommitHash) throws Exception {
        if(ancestorCommitHash==null || ancestorCommitHash.isEmpty())
        {
            return true;
        }

        String current = descendantCommitHash;
        while(current!=null && !current.isEmpty())
        {
            if(current.equals(ancestorCommitHash))
            {
                return true;
            }
            current = Log.deserializeCommit(current).getParentCommitHash();
        }
        return false;
    }

}
