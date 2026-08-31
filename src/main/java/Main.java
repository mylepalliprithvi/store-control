import add.Add;
import branch.Branch;
import checkout.Checkout;
import commit.Commit;
import common.Constants;
import file.FileUtil;
import hash.*;
import init.*;
import log.Log;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {
        int n = args.length;
        String arg0 = "";
        String arg1 = "";
        if(n>0)
        {
            arg0 = args[0];
        }
        if(n>1)
        {
            arg1 = args[1];
        }
        Init init = new Init();
        HashContent hashContent = new HashContent();
        FileUtil fileUtil;
        System.out.println("command selected: "+arg0);

        if(arg1.isEmpty())
        {
            arg1 = "Not Applicable";
        }
        System.out.println("Argument 2: "+arg1);

        if(arg0.equalsIgnoreCase("init"))
        {
            System.out.println("Proceeding to initialize store as command entered is 'store init' ");
            init.init();
        }

        else if(arg0.equals("hash-object"))
        {
            System.out.println("Proceeding to perform hashing object.. as command entered is 'store hash-object' ");
            if(init==null)
            {
                System.out.println("Store not initalized!!");
                return;
            }
            //check file exists
            fileUtil = new FileUtil();
            boolean fileExists = fileUtil.doesFileExist(arg1);
            if(!fileExists)
            {
                System.out.println("File "+arg1+ " does not exist!!");
                return;
            }
            //hash content
            byte[] fileBytes = Files.readAllBytes(Path.of(arg1));
            String hashCode = hashContent.hashObject(fileBytes,"blob");
            System.out.println("Hash code of file: "+hashCode);
        }

        else if(arg0.equals("cat-file"))
        {
            if(init==null)
            {
                System.out.println("Store not initalized!!");
                return;
            }
            System.out.println("Proceeding to read out contents of hash provided as command selected is: 'store cat-file' ");
            String content = hashContent.readObject(arg1);
            System.out.println("Content of hash code "+arg1+" is: "+content);
        }

        else if(arg0.equals("add"))
        {
            if(init==null)
            {
                System.out.println("Store not initalized!!");
                return;
            }

            System.out.println("Proceeding to add file: "+arg1+" to store");
            if(arg1.equals("."))
            {
                addAllFiles(new File("."));
            }
            else{
                Add.addFile(Path.of(arg1));
            }
        }

        else if(arg0.equals("commit"))
        {
            if(init==null)
            {
                System.out.println("store not initialized!!");
                return;
            }
            String commitMessage = "";
            for(int i=1;i<args.length;i++)
            {
                if(args[i].equals("-m") && i+1<args.length)
                {
                    commitMessage = args[i+1];
                    break;
                }
            }
            if(commitMessage.isEmpty())
            {
                System.out.println("Commit message required: use 'store commit -m \"message\"'");
                return;
            }
            System.out.println("Proceeding to commit with message: "+commitMessage);
            Commit.commit(commitMessage);
        }

        else if(arg0.equals("log"))
        {
            if(init==null)
            {
                System.out.println("store not initialized!!");
                return;
            }
            System.out.println("Proceeding to log all the commits as chosen command is: "+arg1);
            Log.log();
        }

        else if(arg0.equals("branch"))
        {
            if(init==null)
            {
                System.out.println("store not initialized!!");
                return;
            }
            if(arg1.equals("Not Applicable"))
            {
                System.out.println("Proceeding to list all branches as command is: "+arg1);
                Branch.listAllBranches();
            }
            else {
                System.out.println("Proceeding to create a new branch with name: "+arg1);
                Branch.branch(arg1);
            }
        }

        else if(arg0.equals("checkout"))
        {
            System.out.println("Proceeding to checkout to branch: "+arg1);
            Checkout.checkout(arg1);
        }
    }

    private static void addAllFiles(File directory) throws Exception
    {
        File[] listOfFiles = directory.listFiles();
        for(File file : listOfFiles)
        {
            if(file.getName().equals(Constants.root) || file.getName().equals(".git"))
            {
                continue;
            }
            if(file.isDirectory())
            {
                addAllFiles(file);
            }
            else
            {
                Add.addFile(file.toPath());
            }
        }
    }




}
