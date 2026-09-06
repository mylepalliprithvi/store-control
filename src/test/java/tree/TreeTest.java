package tree;

import common.Constants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeTest {

    private String originalRoot;

    @BeforeEach
    void redirectStorageRoot(@TempDir Path tempDir) {
        originalRoot = Constants.root;
        Constants.root = tempDir.resolve("store").toString();
    }

    @AfterEach
    void restoreStorageRoot() {
        Constants.root = originalRoot;
    }

    @Test
    void writeTreeAndReadTreeRoundTripsNestedDirectory(@TempDir Path workDir) throws Exception {
        Path subDir = workDir.resolve("sub");
        Files.createDirectories(subDir);
        Files.writeString(workDir.resolve("top.txt"),"top level file");
        Files.writeString(subDir.resolve("nested.txt"),"nested file");

        String treeHash = Tree.writeTree(workDir.toFile());
        Set<TreeEntry> entries = Tree.readTree(treeHash,"");

        long blobCount = entries.stream().filter(e -> e.getFileType().equals("blob")).count();
        assertEquals(2,blobCount);

        assertTrue(entries.stream().anyMatch(e -> e.getFileName().equals("top.txt")));
        assertTrue(entries.stream().anyMatch(e -> e.getFileName().equals("nested.txt")));
    }

    @Test
    void writeTreeFromIndexIsDeterministic() throws Exception {
        JSONArray index = new JSONArray();
        index.put(new JSONObject().put("path","a.txt").put("hash","aaaa"));
        index.put(new JSONObject().put("path","b.txt").put("hash","bbbb"));

        String hash1 = Tree.writeTreeFromIndex(index);
        String hash2 = Tree.writeTreeFromIndex(index);

        assertEquals(hash1,hash2);
    }

    @Test
    void writeTreeFromIndexProducesDifferentHashForDifferentContent() throws Exception {
        JSONArray indexA = new JSONArray();
        indexA.put(new JSONObject().put("path","a.txt").put("hash","aaaa"));

        JSONArray indexB = new JSONArray();
        indexB.put(new JSONObject().put("path","a.txt").put("hash","zzzz"));

        String hashA = Tree.writeTreeFromIndex(indexA);
        String hashB = Tree.writeTreeFromIndex(indexB);

        assertNotEquals(hashA,hashB);
    }
}
