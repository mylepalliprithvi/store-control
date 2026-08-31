package tree;

import java.nio.file.Path;
import java.util.Objects;

public class TreeEntry {
    public final String fileMode;
    public final String fileType;
    public final String fileContentHash;
    public final String fileName;
    public final Path filePath;
    public TreeEntry(String fileMode,String fileType,String fileContentHash,String fileName,Path filePath) {
        this.fileMode = fileMode;
        this.fileType = fileType;
        this.fileContentHash = fileContentHash;
        this.fileName = fileName;
        this.filePath = filePath;
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

    public Path getFilePath() {
        return filePath;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TreeEntry treeEntry = (TreeEntry) o;
        return Objects.equals(fileMode, treeEntry.fileMode) && Objects.equals(fileType, treeEntry.fileType) && Objects.equals(fileContentHash, treeEntry.fileContentHash) && Objects.equals(fileName, treeEntry.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileMode, fileType, fileContentHash, fileName);
    }
}
