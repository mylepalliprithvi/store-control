package index;

public class IndexModel {
    String path;
    String type;
    String hash;

    public IndexModel(String path,String type,String hash)
    {
        this.path = path;
        this.type = type;
        this.hash = hash;
    }
    public String getHash() {
        return hash;
    }

    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setType(String type) {
        this.type = type;
    }
}
