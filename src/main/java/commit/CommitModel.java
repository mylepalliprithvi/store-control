package commit;

public class CommitModel {
    public String commitHash;
    public String commitAuthorName;
    public String commitAuthorEmail;
    public String parentCommitHash;
    public String commitTimeStamp;
    public String commitMessage;
    public String treeHash;

    // commitHash isn't a constructor param: it's the hash of this object's
    // own serialized content, so it's only known after hashing/storing it.
    // Set model.commitHash on the instance afterward (see Commit.commit()).
    public CommitModel(String commitAuthorName, String commitAuthorEmail, String parentCommitHash, String commitTimeStamp,String commitMessage,String treeHash)
    {
        this.commitAuthorName = commitAuthorName;
        this.commitAuthorEmail = commitAuthorEmail;
        this.parentCommitHash = parentCommitHash;
        this.commitTimeStamp = commitTimeStamp;
        this.commitMessage = commitMessage;
        this.treeHash = treeHash;
    }

    public void setCommitAuthorEmail(String commitAuthorEmail) {
        this.commitAuthorEmail = commitAuthorEmail;
    }

    public void setParentCommitHash(String parentCommitHash) {
        this.parentCommitHash = parentCommitHash;
    }

    public String getCommitTimeStamp() {
        return commitTimeStamp;
    }

    public String getCommitAuthorName() {
        return commitAuthorName;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public String getCommitAuthorEmail() {
        return commitAuthorEmail;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    public String getTreeHash() {
        return treeHash;
    }

    public String getParentCommitHash() {
        return parentCommitHash;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitAuthorName(String commitAuthorName) {
        this.commitAuthorName = commitAuthorName;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public void setCommitTimeStamp(String commitTimeStamp) {
        this.commitTimeStamp = commitTimeStamp;
    }

    public void setTreeHash(String treeHash) {
        this.treeHash = treeHash;
    }
}
