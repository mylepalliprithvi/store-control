package hash;

import common.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class HashContentTest {

    private String originalRoot;

    @BeforeEach
    void redirectStorageRoot(@TempDir Path tempDir) {
        originalRoot = Constants.root;
        Constants.root = tempDir.toString();
    }

    @AfterEach
    void restoreStorageRoot() {
        Constants.root = originalRoot;
    }

    @Test
    void hashObjectRoundTripsOriginalContent() throws Exception {
        byte[] content = "hello store".getBytes(StandardCharsets.UTF_8);
        String hash = HashContent.hashObject(content,"blob");
        String readBack = HashContent.readObject(hash);
        assertEquals("hello store",readBack);
    }

    @Test
    void sameContentProducesSameHash() throws Exception {
        byte[] content = "identical content".getBytes(StandardCharsets.UTF_8);
        String hash1 = HashContent.hashObject(content,"blob");
        String hash2 = HashContent.hashObject(content,"blob");
        assertEquals(hash1,hash2);
    }

    @Test
    void differentContentProducesDifferentHash() throws Exception {
        String hash1 = HashContent.hashObject("content A".getBytes(StandardCharsets.UTF_8),"blob");
        String hash2 = HashContent.hashObject("content B".getBytes(StandardCharsets.UTF_8),"blob");
        assertNotEquals(hash1,hash2);
    }

    @Test
    void computeHashMatchesHashObjectWithoutWritingToDisk() throws Exception {
        byte[] content = "no side effects please".getBytes(StandardCharsets.UTF_8);
        String computed = HashContent.computeHash(content,"blob");
        String stored = HashContent.hashObject(content,"blob");
        assertEquals(stored,computed);
    }

    @Test
    void differentTypeProducesDifferentHashForSameContent() throws Exception {
        byte[] content = "same bytes".getBytes(StandardCharsets.UTF_8);
        String blobHash = HashContent.hashObject(content,"blob");
        String treeHash = HashContent.hashObject(content,"tree");
        assertNotEquals(blobHash,treeHash);
    }
}
