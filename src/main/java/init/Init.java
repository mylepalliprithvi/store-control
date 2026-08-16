package init;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    }
}
