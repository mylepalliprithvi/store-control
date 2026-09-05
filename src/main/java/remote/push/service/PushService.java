package remote.push.service;
import common.Constants;
import log.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import remote.push.model.PushRequestModel;
import remote.push.model.PushResponseModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
public class PushService {
    private static final Logger LOGGER = LogManager.getLogger(PushService.class);

    public PushResponseModel pushCommit(String branchName,String commitHash, PushRequestModel request) throws Exception {
        Path headsPath = Path.of(Constants.heads);
        Path localBranchPath = Path.of(headsPath+"/"+branchName);

        if(Files.notExists(localBranchPath))
        {
            PushResponseModel errorResponse = new PushResponseModel();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Rejected. Branch does not exist locally");
            errorResponse.setBranchName(branchName);
            return errorResponse;
        }

        String latestLocalCommitHash = getLatestCommitOnBranch(branchName);

        if(!latestLocalCommitHash.equals(commitHash))
        {
            LOGGER.error("Commit: {} is not the latest commit to push to remote. Please check",commitHash);
            PushResponseModel errorResponseModel = new PushResponseModel();
            errorResponseModel.setBranchName(branchName);
            errorResponseModel.setSuccess(false);
            errorResponseModel.setMessage("Rejected: commitHash does not match local branch tip");
            errorResponseModel.setPreviousCommitHash(latestLocalCommitHash);
            return errorResponseModel;
        }
        Path remoteOriginPath = Path.of(Constants.remoteOriginPath);
        Path remoteBranchPath = Path.of(remoteOriginPath+"/"+branchName);
        String previousRemoteCommitHash = Files.exists(remoteBranchPath)?Files.readString(remoteBranchPath) : null;
        if(Files.notExists(remoteOriginPath))
        {
            Files.createDirectories(remoteOriginPath);
        }
        Map<String,byte[]> files = request.getObjects();
        if(files==null)
        {
            throw new NullPointerException("Push request does not contain any files");
        }
        for(Map.Entry<String,byte[]> entry : files.entrySet())
        {
            String hash = entry.getKey();
            Path objPath = Path.of(Constants.root+"/"+Constants.objects,hash.substring(0,2),hash.substring(2));
            if(Files.notExists(objPath))
            {
                Files.createDirectories(objPath.getParent());
                Files.write(objPath,entry.getValue());
            }
        }
        if(!isFastForward(commitHash, previousRemoteCommitHash))
        {
            LOGGER.error("Push rejected: {} is not a fast-forward of remote's current tip {}",commitHash,previousRemoteCommitHash);
            PushResponseModel rejectedResponseModel = new PushResponseModel();
            rejectedResponseModel.setSuccess(false);
            rejectedResponseModel.setBranchName(branchName);
            rejectedResponseModel.setPreviousCommitHash(previousRemoteCommitHash);
            rejectedResponseModel.setMessage("Rejected: non-fast-forward, remote has diverged");
            return rejectedResponseModel;
        }
        Files.writeString(remoteBranchPath,latestLocalCommitHash);
        LOGGER.info("Successfully pushed commit: {} to remote.",commitHash);
        PushResponseModel successResponseModel = new PushResponseModel();
        successResponseModel.setSuccess(true);
        successResponseModel.setMessage("Accepted: commit successfully pushed to remote!");
        successResponseModel.setPreviousCommitHash(previousRemoteCommitHash);
        successResponseModel.setBranchName(branchName);
        successResponseModel.setNewCommitHash(commitHash);
        return successResponseModel;
    }

    private boolean isFastForward(String newCommitHash, String previousRemoteCommitHash) throws Exception {
        if(previousRemoteCommitHash == null)
        {
            return true;
        }
        String current = newCommitHash;
        while(current != null && !current.isEmpty())
        {
            if(current.equals(previousRemoteCommitHash))
            {
                return true;
            }
            current = Log.deserializeCommit(current).getParentCommitHash();
        }
        return false;
    }

    public String getLatestCommitOnBranch(String branchName) throws IOException {
        Path headsPath = Path.of(Constants.heads);
        Path branchPath = Path.of(headsPath+"/"+branchName);
        LOGGER.info("Latest commit local on branch: {} is {}",branchName,Files.readString(branchPath));
        return Files.readString(branchPath);
    }

}
