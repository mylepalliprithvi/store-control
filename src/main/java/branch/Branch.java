package branch;

import common.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class Branch {

    static String headsPathString = Constants.root+"/refs/heads/";
    public static void branch(String branchName) throws IOException {
        if(branchName.isEmpty())
        {
            listAllBranches();
            return;
        }

        System.out.println("Creating branch: "+branchName);
        Path currentRefPath = Ref.getCurrentBranchRefPath();
        String currentRefPathString = String.valueOf(currentRefPath);
        String currentBranch = currentRefPathString.split("/")[3];
        System.out.println("Current on branch: "+currentBranch);
        Path latestCommitRefPath = Path.of(currentRefPathString);
        String latestCommitOnCurrentBranch = Files.readString(latestCommitRefPath);


        Path newBranchPath = Path.of(headsPathString+branchName);
        Files.createDirectories(newBranchPath.getParent());
        Files.writeString(newBranchPath,latestCommitOnCurrentBranch);
    }

    public static void listAllBranches(){
        Path headsPath = Path.of(headsPathString);
        try{
            String currentBranch = Ref.getCurrentBranchRefPath().getFileName().toString();
            try (Stream<Path> listOfBranches = Files.list(headsPath)) {
                listOfBranches
                        .map(path -> path.getFileName().toString())
                        .sorted()
                        .forEach(name -> {
                            String marker = name.equals(currentBranch) ? "* " : "  ";
                            System.out.println(marker + name);
                        });
            }
        }
        catch(Exception e)
        {
            System.out.println("Encountered exception "+e+" when listing branches");
        }
    }


}
