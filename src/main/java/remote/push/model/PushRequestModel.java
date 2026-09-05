package remote.push.model;

import java.util.Map;

public class PushRequestModel {
    public String branchName;
    public String commitHash;
    private Map<String,byte[]> objects;
    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public String getBranchName() {
        return branchName;
    }

    public Map<String, byte[]> getObjects() {
        return objects;
    }

    public void setObjects(Map<String, byte[]> objects) {
        this.objects = objects;
    }
}
