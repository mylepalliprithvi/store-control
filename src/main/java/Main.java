import add.Add;
import commit.Commit;
import common.Constants;
import file.FileUtil;
import hash.*;
import init.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) throws Exception {
        int n = args.length;
        String arg0 = "";
        String arg1 = "";
        String arg2 = "";
        Init init;
        HashContent hashContent;
        FileUtil fileUtil;
        if(n>0)
        {
            arg0 = args[0];
        }
        System.out.println("Tool selected: "+arg0);
        if(arg0.equalsIgnoreCase("store"))
        {
            init = new Init();
            hashContent = new HashContent();
        }
        else {
            init = null;
            System.out.println("Tool selected is not store, hence skip store process");
            return;
        }
        if(n>1)
        {
            arg1 = args[1];
        }
        System.out.println("command selected: "+arg1);
        if(n>2)
        {
            arg2 = args[2];

        }
        System.out.println("Argument 2: "+arg2);

        if(arg1.equalsIgnoreCase("init"))
        {
            System.out.println("Proceeding to initialize store as command entered is 'store init' ");
            init.init();
        }

        if(arg1.equals("hash-object") && !arg2.isEmpty())
        {
            System.out.println("Proceeding to perform hashing object.. as command entered is 'store hash-object' ");
            if(init==null)
            {
                System.out.println("Store not initalized!!");
                return;
            }
            //check file exists
            fileUtil = new FileUtil();
            boolean fileExists = fileUtil.doesFileExist(arg2);
            if(!fileExists)
            {
                System.out.println("File "+arg2+ " does not exist!!");
                return;
            }
            //hash content
            byte[] fileBytes = Files.readAllBytes(Path.of(arg2));
            String hashCode = hashContent.hashObject(fileBytes,"blob");
            System.out.println("Hash code of file: "+hashCode);
        }

        if(arg1.equals("cat-file") && !arg2.isEmpty())
        {
            if(init==null)
            {
                System.out.println("Store not initalized!!");
                return;
            }
            System.out.println("Proceeding to read out contents of hash provided as command selected is: 'store cat-file' ");
            String content = hashContent.readObject(arg2);
            System.out.println("Content of hash code "+arg2+" is: "+content);
        }

        if(arg1.equals("add"))
        {
            if(init==null)
            {
                System.out.println("Store not initalized!!");
                return;
            }

            System.out.println("Proceeding to add file: "+arg2+" to store");
            if(arg2.equals("."))
            {
                addAllFiles(new File("."));
            }
            else{
                Add.addFile(Path.of(arg2));
            }
        }

        if(arg1.equals("commit") && !arg2.isEmpty())
        {
            System.out.println("Proceeding to commit as chosen arg1: "+arg1);
            Commit.commit(arg2);
        }
    }

    private static void addAllFiles(File directory) throws Exception
    {
        File[] listOfFiles = directory.listFiles();
        for(File file : listOfFiles)
        {
            if(file.getName().equals(Constants.root))
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
