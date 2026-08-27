package branch;

import common.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Ref {
    // HEAD holds "ref: refs/heads/<branch>" (a pointer to a branch, not a
    // commit directly) - resolve through HEAD every time rather than
    // hardcoding "main", so this works on whatever branch is checked out.
    public static Path getCurrentBranchRefPath() throws IOException {
        Path headPath = Path.of(Constants.root, "HEAD");
        String headContents = Files.readString(headPath).trim();
        String branchRef = headContents.replaceFirst("^ref:\\s*", "");
        return Path.of(Constants.root, branchRef);
    }
}
