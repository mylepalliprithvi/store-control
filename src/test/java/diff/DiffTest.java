package diff;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiffTest {

    private void assertRoundTrips(List<String> oldLines, List<String> newLines) {
        List<Diff.Edit> edits = Diff.diffLines(oldLines,newLines);
        List<String> reconstructedOld = new ArrayList<>();
        List<String> reconstructedNew = new ArrayList<>();
        for(Diff.Edit edit : edits)
        {
            if(edit.type == Diff.EditType.EQUAL)
            {
                reconstructedOld.add(edit.line);
                reconstructedNew.add(edit.line);
            }
            else if(edit.type == Diff.EditType.DELETE)
            {
                reconstructedOld.add(edit.line);
            }
            else
            {
                reconstructedNew.add(edit.line);
            }
        }
        assertEquals(oldLines,reconstructedOld,"edit script does not reconstruct the old content");
        assertEquals(newLines,reconstructedNew,"edit script does not reconstruct the new content");
    }

    @Test
    void identicalContentProducesOnlyEqualEdits() {
        List<String> lines = Arrays.asList("a","b","c");
        List<Diff.Edit> edits = Diff.diffLines(lines,lines);
        assertTrue(edits.stream().allMatch(e -> e.type == Diff.EditType.EQUAL));
        assertRoundTrips(lines,lines);
    }

    @Test
    void emptyToEmptyProducesNoEdits() {
        List<Diff.Edit> edits = Diff.diffLines(new ArrayList<>(),new ArrayList<>());
        assertTrue(edits.isEmpty());
    }

    @Test
    void emptyToNonEmptyIsAllInserts() {
        List<String> newLines = Arrays.asList("x","y");
        assertRoundTrips(new ArrayList<>(),newLines);
        List<Diff.Edit> edits = Diff.diffLines(new ArrayList<>(),newLines);
        assertTrue(edits.stream().allMatch(e -> e.type == Diff.EditType.INSERT));
    }

    @Test
    void nonEmptyToEmptyIsAllDeletes() {
        List<String> oldLines = Arrays.asList("x","y");
        assertRoundTrips(oldLines,new ArrayList<>());
        List<Diff.Edit> edits = Diff.diffLines(oldLines,new ArrayList<>());
        assertTrue(edits.stream().allMatch(e -> e.type == Diff.EditType.DELETE));
    }

    @Test
    void middleInsertionRoundTrips() {
        assertRoundTrips(Arrays.asList("1","2","5"),Arrays.asList("1","2","3","4","5"));
    }

    @Test
    void middleDeletionRoundTrips() {
        assertRoundTrips(Arrays.asList("1","2","3","4","5"),Arrays.asList("1","5"));
    }

    @Test
    void classicMyersExampleRoundTrips() {
        assertRoundTrips(
                Arrays.asList("A","B","C","A","B","B","A"),
                Arrays.asList("C","B","A","B","A","C")
        );
    }

    @Test
    void singleLineReplacementIsDeleteThenInsert() {
        List<Diff.Edit> edits = Diff.diffLines(Arrays.asList("hello"),Arrays.asList("world"));
        assertEquals(2,edits.size());
        assertEquals(Diff.EditType.DELETE,edits.get(0).type);
        assertEquals(Diff.EditType.INSERT,edits.get(1).type);
    }
}
