package file;

import java.io.File;

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
}
