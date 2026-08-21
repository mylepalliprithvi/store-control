package tree;

import java.io.Serializable;

public class TreeEntry {
    public final String fileMode;
    public final String fileType;
    public final String fileContentHash;
    public final String fileName;
    public TreeEntry(String fileMode,String fileType,String fileContentHash,String fileName) {
        this.fileMode = fileMode;
        this.fileType = fileType;
        this.fileContentHash = fileContentHash;
        this.fileName = fileName;
    }

    public String getFileContentHash() {
        return fileContentHash;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileMode() {
        return fileMode;
    }

    public String getFileType() {
        return fileType;
    }
}
