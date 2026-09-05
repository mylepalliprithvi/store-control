package remote.push.model;

public class PushResponseModel {
    private boolean isSuccess;
    private String branchName;
    private String previousCommitHash;
    private String newCommitHash;
    private String message;

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public void setSuccess(boolean success) {
        this.isSuccess = success;
    }

    public void setPreviousCommitHash(String commitHash)
    {
        this.previousCommitHash = commitHash;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setNewCommitHash(String newCommitHash) {
        this.newCommitHash = newCommitHash;
    }

    public String getBranchName() {
        return branchName;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public String getMessage() {
        return message;
    }

    public String getNewCommitHash() {
        return newCommitHash;
    }

    public String getPreviousCommitHash() {
        return previousCommitHash;
    }
}
