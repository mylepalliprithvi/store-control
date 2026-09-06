package file;

import common.Constants;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {

    public static boolean doesFileExist(String filePath)
    {
        try{
            File file = new File(filePath);
            return file.exists();
        }catch(Exception e)
        {
            System.out.println("Encountered exception: "+e);
        }
        System.out.println("Reached after try and catch, hence going to return false");
        return false;
    }

    // Recursively lists every file under directory, skipping the store's own
    // metadata dir, .git, and build output - the same skip-list every
    // directory walk in this project needs (add ., status, ...).
    public static List<Path> collectTrackableFiles(File directory) {
        List<Path> result = new ArrayList<>();
        collectTrackableFiles(directory,result);
        return result;
    }

    private static void collectTrackableFiles(File directory, List<Path> result) {
        File[] files = directory.listFiles();
        if(files == null)
        {
            return;
        }
        for(File file : files)
        {
            if(file.getName().equals(Constants.root) || file.getName().equals(".git") || file.getName().equals("target"))
            {
                continue;
            }
            if(file.isDirectory())
            {
                collectTrackableFiles(file,result);
            }
            else
            {
                result.add(file.toPath());
            }
        }
    }
}
