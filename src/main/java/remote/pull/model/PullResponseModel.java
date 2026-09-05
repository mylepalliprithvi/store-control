package remote.pull.model;

public class PullResponseModel {
    public String commitHash;
    public boolean isSuccess;
    public String message;
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
}
