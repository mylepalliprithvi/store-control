package checkout;

import branch.Ref;
import commit.Commit;
import common.Constants;
import hash.HashContent;
import tree.Tree;
import tree.TreeEntry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class Checkout {

    public static void checkout(String branchToCheckout) throws Exception {
        Set<TreeEntry> directoriesInCurrentBranch = new HashSet<>();
        Set<TreeEntry> directoriesInBranchToMove = new HashSet<>();
        Map<String, TreeEntry> currentBranchMap = new HashMap<>();
        Map<String, TreeEntry> checkoutBranchMap = new HashMap<>();
        boolean branchToCheckoutExists;
        Path headsPath = Path.of(Constants.heads);
        try (Stream<Path> listOfFiles = Files.list(headsPath)) {
            branchToCheckoutExists = listOfFiles.anyMatch(f -> f.getFileName().toString().equals(branchToCheckout));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (!branchToCheckoutExists) {
            System.out.println("Branch '"+branchToCheckout+"' does not exist!!");
            return;
        }

        Path branchToCheckoutPath = headsPath.resolve(branchToCheckout);
        String lastHashOnBranch = Files.readString(branchToCheckoutPath);
        directoriesInBranchToMove = Tree.readTree(Commit.getTreeHashFromCommit(lastHashOnBranch),"");
        for (TreeEntry e : directoriesInBranchToMove) {
            checkoutBranchMap.put(e.getFilePath().toString(), e);
        }

        Path currentBranchPath = Ref.getCurrentBranchRefPath();
        String lastHashOnCurrentBranch = Files.readString(currentBranchPath);
        directoriesInCurrentBranch = Tree.readTree(Commit.getTreeHashFromCommit(lastHashOnCurrentBranch),"");
        for (TreeEntry entry : directoriesInCurrentBranch) {
            currentBranchMap.put(entry.getFilePath().toString(), entry);
        }
        //Add files in this tree to set directoriesInCurrentBranch

        //compare two sets and remove the items present in set directoriesInCurrentBranch but not in directoriesInBranchToMove from index.json.

        for (Map.Entry<String, TreeEntry> entryMap : checkoutBranchMap.entrySet()) {
            String filePath = entryMap.getKey();
            TreeEntry treeEntry = entryMap.getValue();
            TreeEntry currentBranchEntry = currentBranchMap.get(filePath);
            if(treeEntry.getFileType().equals("tree"))
            {
                continue;
            }
            if(currentBranchEntry==null)
            {
                //create the file in that directory
                Path parent = treeEntry.getFilePath().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(treeEntry.getFilePath(),HashContent.readObject(treeEntry.getFileContentHash()));
            }
            else if(!currentBranchEntry.getFileContentHash().equals(treeEntry.getFileContentHash()))
            {
                //make current branch entry to the checkout branch entry
                Files.writeString(treeEntry.getFilePath(),HashContent.readObject(treeEntry.getFileContentHash()));
            }
            else {
                //nothing to do, ignore
            }
        }

        for(Map.Entry<String,TreeEntry> e : currentBranchMap.entrySet())
        {
            String filePath = e.getKey();
            TreeEntry currentBranchEntry = currentBranchMap.get(filePath);
            TreeEntry checkoutBranchEntry = checkoutBranchMap.get(filePath);

            if(checkoutBranchEntry==null && currentBranchEntry.getFileType().equals("blob"))
            {
                //remove file from disk
                Files.deleteIfExists(currentBranchEntry.getFilePath());
            }

        }

        //finally update .store/HEAD with branchToCheckout
        Path headPath = Path.of(Constants.root,"HEAD");
        String headContent = "ref: refs/heads/"+branchToCheckout;
        Files.writeString(headPath,headContent);
    }


}
