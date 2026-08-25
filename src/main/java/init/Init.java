package init;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;

import common.Constants;

public class Init {
    public static void init() throws IOException {
        Path path = Paths.get(Constants.root);
        //check if path exists
        //if it does then return
        if(Files.exists(path))
        {
            System.out.println("Store already initialized...");
            return;
        }
        //if not then initialize the path
        //create objects folder, refs folder and HEAD
        Path basePath = Files.createDirectories(path);
        Files.createDirectories(path.resolve(Constants.objects));
        //creates refs folder which again has heads folder. heads folder will have one folder for every branch created
        Files.createDirectories(basePath.resolve("refs").resolve("heads"));
        Path mainFile = path.resolve("HEAD");
        Files.writeString(mainFile,"ref: refs/heads/main\n");
        /*
            creates index.json which is used to track added and not added contents
         */
        Path indexFile = path.resolve("index.json");
        try{
            Files.createFile(indexFile);
            Files.writeString(indexFile,"[]");
        }
        catch(FileAlreadyExistsException e)
        {
            System.err.println("File index.json exists already!!");
        }

        /*
            creates config.json file for storing user details
         */
        Path configFile = path.resolve("config.json");
        try{
            Files.createFile(configFile);
            Files.writeString(configFile,"{}");
        }
        catch(FileAlreadyExistsException e)
        {
            System.err.println("File config.json exists already!!");
        }
    }
}
