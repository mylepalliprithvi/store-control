package remote.pull.model;

import java.util.Map;

public class PullResponseModel {
    public String commitHash;
    public boolean isSuccess;
    public String message;
    public String branchName;
    public Map<String, byte[]> objects;

    public void setCommitHash(String commitHash)
    {
        this.commitHash = commitHash;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public Map<String, byte[]> getObjects() {
        return objects;
    }

    public void setObjects(Map<String, byte[]> objects) {
        this.objects = objects;
    }
}
