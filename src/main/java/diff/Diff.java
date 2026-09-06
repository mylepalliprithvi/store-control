package diff;

import branch.Ref;
import commit.Commit;
import hash.HashContent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Diff {

    public enum EditType { EQUAL, INSERT, DELETE }

    public static class Edit {
        public final EditType type;
        public final String line;
        public Edit(EditType type, String line) {
            this.type = type;
            this.line = line;
        }
    }

    // Compares path's HEAD-committed content against its current working
    // directory content and prints a git-style +/- diff.
    public static void diff(String pathArg) throws Exception {
        Path path = Path.of(pathArg);
        String pathString = path.toString();

        Path branchRefPath = Ref.getCurrentBranchRefPath();
        String headHash = Files.exists(branchRefPath) ? Files.readString(branchRefPath).trim() : "";
        Map<String,String> headBlobPaths = Commit.getBlobPathsForCommit(headHash);
        String headBlobHash = headBlobPaths.get(pathString);

        if(headBlobHash == null && Files.notExists(path))
        {
            System.out.println("Path '"+pathString+"' not found in HEAD or working directory");
            return;
        }

        String oldContent = headBlobHash != null ? HashContent.readObject(headBlobHash) : "";
        String newContent = Files.exists(path) ? Files.readString(path) : "";

        List<String> oldLines = splitLines(oldContent);
        List<String> newLines = splitLines(newContent);

        System.out.println("diff -- "+pathString);
        for(Edit edit : diffLines(oldLines,newLines))
        {
            if(edit.type == EditType.EQUAL)
            {
                System.out.println("  "+edit.line);
            }
            else if(edit.type == EditType.DELETE)
            {
                System.out.println("- "+edit.line);
            }
            else
            {
                System.out.println("+ "+edit.line);
            }
        }
    }

    private static List<String> splitLines(String content) {
        if(content.isEmpty())
        {
            return new ArrayList<>();
        }
        return Arrays.asList(content.split("\n"));
    }

    // Myers' O(ND) shortest-edit-script algorithm: the minimal set of line
    // insertions/deletions that turns oldLines into newLines.
    public static List<Edit> diffLines(List<String> oldLines, List<String> newLines) {
        int n = oldLines.size();
        int m = newLines.size();
        int max = n + m;
        if(max == 0)
        {
            return new ArrayList<>();
        }
        int offset = max;
        int[] v = new int[2*max+1];
        List<int[]> trace = new ArrayList<>();

        v[offset+1] = 0;
        searchLoop:
        for(int d = 0; d <= max; d++)
        {
            trace.add(v.clone());
            for(int k = -d; k <= d; k += 2)
            {
                int x;
                if(k == -d || (k != d && v[offset+k-1] < v[offset+k+1]))
                {
                    x = v[offset+k+1];
                }
                else
                {
                    x = v[offset+k-1] + 1;
                }
                int y = x - k;
                while(x < n && y < m && oldLines.get(x).equals(newLines.get(y)))
                {
                    x++;
                    y++;
                }
                v[offset+k] = x;
                if(x >= n && y >= m)
                {
                    break searchLoop;
                }
            }
        }

        List<Edit> reversedEdits = new ArrayList<>();
        int x = n;
        int y = m;
        for(int depth = trace.size()-1; depth >= 0; depth--)
        {
            int[] vAtDepth = trace.get(depth);
            int k = x - y;
            int prevK;
            if(k == -depth || (k != depth && vAtDepth[offset+k-1] < vAtDepth[offset+k+1]))
            {
                prevK = k+1;
            }
            else
            {
                prevK = k-1;
            }
            int prevX = vAtDepth[offset+prevK];
            int prevY = prevX - prevK;

            while(x > prevX && y > prevY)
            {
                reversedEdits.add(new Edit(EditType.EQUAL,oldLines.get(x-1)));
                x--;
                y--;
            }

            if(depth > 0)
            {
                if(x == prevX)
                {
                    reversedEdits.add(new Edit(EditType.INSERT,newLines.get(prevY)));
                }
                else
                {
                    reversedEdits.add(new Edit(EditType.DELETE,oldLines.get(prevX)));
                }
            }
            x = prevX;
            y = prevY;
        }

        Collections.reverse(reversedEdits);
        return reversedEdits;
    }
}
