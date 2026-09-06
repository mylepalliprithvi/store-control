package remote.pull.service;
import commit.CommitGraph;
import common.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import remote.pull.model.PullResponseModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
public class PullService {
    private static final Logger LOGGER = LogManager.getLogger(PullService.class);

    public PullResponseModel pullCommits(String branchName, String localCommitHash) throws Exception {
        Path remoteBranchPath = Path.of(Constants.remoteOriginPath+"/"+branchName);
        if(Files.notExists(remoteBranchPath))
        {
            LOGGER.error("Pull service failed: Branch: {} does not have an upstream remote branch",branchName);
            PullResponseModel errorResponseModel = new PullResponseModel();
            errorResponseModel.setSuccess(false);
            errorResponseModel.setBranchName(branchName);
            errorResponseModel.setMessage("Rejected. Branch does not exist in remote");
            return errorResponseModel;
        }

        String remoteLatestCommitHash = Files.readString(remoteBranchPath);

        if(remoteLatestCommitHash.equals(localCommitHash))
        {
            PullResponseModel responseModel = new PullResponseModel();
            responseModel.setSuccess(true);
            responseModel.setBranchName(branchName);
            responseModel.setMessage("Success. Already up to date");
            responseModel.setCommitHash(remoteLatestCommitHash);
            responseModel.setObjects(Map.of());
            return responseModel;
        }

        if(!isAncestor(localCommitHash,remoteLatestCommitHash))
        {
            LOGGER.error("Pull rejected. Local commit: {} diverged from Remote commit: {}. Please merge!",localCommitHash,remoteLatestCommitHash);
            PullResponseModel errorResponseModel = new PullResponseModel();
            errorResponseModel.setSuccess(false);
            errorResponseModel.setBranchName(branchName);
            errorResponseModel.setMessage("Rejected. Merging required as branches have diverged");
            return errorResponseModel;
        }

        Map<String,byte[]> objects = CommitGraph.collectObjects(remoteLatestCommitHash,localCommitHash);

        PullResponseModel successResponseModel = new PullResponseModel();
        successResponseModel.setSuccess(true);
        successResponseModel.setBranchName(branchName);
        successResponseModel.setMessage("Success. New commits available");
        successResponseModel.setCommitHash(remoteLatestCommitHash);
        successResponseModel.setObjects(objects);
        LOGGER.info("Gathered {} objects for branch: {} pull",objects.size(),branchName);
        return successResponseModel;
    }

    private boolean isAncestor(String ancestorCommitHash, String descendantCommitHash) throws Exception {
        return CommitGraph.isAncestor(ancestorCommitHash, descendantCommitHash);
    }

}
