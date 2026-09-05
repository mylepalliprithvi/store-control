package remote;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import remote.push.service.PushService;
import remote.push.model.PushRequestModel;
import remote.push.model.PushResponseModel;
import remote.pull.model.PullResponseModel;
import remote.pull.service.PullService;

@RestController
@RequestMapping("/origin")
public class RemoteController {

    private static final Logger logger = LogManager.getLogger(RemoteController.class);

    @Autowired
    public PushService pushService;

    @Autowired
    public PullService pullService;

    @PostMapping("/push/{branch}/{commitHash}")
    public ResponseEntity<PushResponseModel> pushCommitToOrigin(@PathVariable("branch") String branchName, @PathVariable("commitHash") String commitHash, @RequestBody PushRequestModel pushRequestModel) throws Exception {
        logger.info("Pushing commit: {} to branch {}",commitHash,branchName);
        return ResponseEntity.ok(pushService.pushCommit(branchName,commitHash,pushRequestModel));
    }

    @GetMapping("/pull/{branch}")
    public ResponseEntity<PullResponseModel> pullCommitFromOrigin(@PathVariable("branch") String branchName) throws Exception {
        logger.info("Pulling commits into branch: {} from branch: origin/{}",branchName,branchName);
        return ResponseEntity.ok(pullService.pullCommits(branchName));
    }
}
