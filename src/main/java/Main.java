import add.Add;
import branch.Branch;
import checkout.Checkout;
import commit.Commit;
import common.Constants;
import config.Config;
import diff.Diff;
import file.FileUtil;
import hash.HashContent;
import init.Init;
import log.Log;
import merge.Merge;
import pull.Pull;
import push.Push;
import status.Status;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name="store", mixinStandardHelpOptions = true, description = "A Git-like version control system.")
public class Main implements Runnable {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        System.out.println("Usage: store <command> [args]. Run 'store --help' for a list of commands.");
    }

    @Command(name="init", mixinStandardHelpOptions = true, description = "Create a .store repository in the current directory")
    void initialize() throws Exception {
        System.out.println("Proceeding to initialize store as command entered is 'store init' ");
        Init.init();
    }

    @Command(name="hash-object", mixinStandardHelpOptions = true, description = "Hash and store a file as a blob, print its hash")
    void hashObject(@Parameters(index="0", paramLabel="<file>", description="file to hash") String file) throws Exception {
        if(!isInitialized())
        {
            System.out.println("Store not initalized!!");
            return;
        }
        System.out.println("Proceeding to perform hashing object.. as command entered is 'store hash-object' ");
        if(!FileUtil.doesFileExist(file))
        {
            System.out.println("File "+file+" does not exist!!");
            return;
        }
        byte[] fileBytes = Files.readAllBytes(Path.of(file));
        String hashCode = HashContent.hashObject(fileBytes,"blob");
        System.out.println("Hash code of file: "+hashCode);
    }

    @Command(name="cat-file", mixinStandardHelpOptions = true, description = "Print the decompressed content of an object")
    void catFile(@Parameters(index="0", paramLabel="<hash>", description="hash to read") String hash) throws Exception {
        if(!isInitialized())
        {
            System.out.println("Store not initalized!!");
            return;
        }
        System.out.println("Proceeding to read out contents of hash provided as command selected is: 'store cat-file' ");
        String content = HashContent.readObject(hash);
        System.out.println("Content of hash code "+hash+" is: "+content);
    }

    @Command(name="add", mixinStandardHelpOptions = true, description = "Stage a file, or '.' for everything not excluded by .storeignore")
    void add(@Parameters(index="0", paramLabel="<file>", description="file to add, or '.' for everything") String fileName) throws Exception {
        if(!isInitialized())
        {
            System.out.println("Store not initalized!!");
            return;
        }
        System.out.println("Proceeding to add file: "+fileName+" to store");
        if(fileName.equals("."))
        {
            addAllFiles(new File("."));
        }
        else
        {
            Add.addFile(Path.of(fileName));
        }
    }

    @Command(name="commit", mixinStandardHelpOptions = true, description = "Commit the staged snapshot")
    void commit(@Option(names="-m", required=true, paramLabel="<message>", description="commit message") String message) throws Exception {
        if(!isInitialized())
        {
            System.out.println("store not initialized!!");
            return;
        }
        System.out.println("Proceeding to commit with message: "+message);
        Commit.commit(message);
    }

    @Command(name="log", mixinStandardHelpOptions = true, description = "Show commit history for the current branch")
    void log() throws Exception {
        if(!isInitialized())
        {
            System.out.println("store not initialized!!");
            return;
        }
        System.out.println("Proceeding to log all the commits");
        Log.log();
    }

    @Command(name="branch", mixinStandardHelpOptions = true, description = "List branches, or create a new one from the current commit")
    void branch(@Parameters(index="0", arity="0..1", paramLabel="<name>", description="new branch name; omit to list branches") String name) throws Exception {
        if(!isInitialized())
        {
            System.out.println("store not initialized!!");
            return;
        }
        if(name == null)
        {
            System.out.println("Proceeding to list all branches");
            Branch.listAllBranches();
        }
        else
        {
            System.out.println("Proceeding to create a new branch with name: "+name);
            Branch.branch(name);
        }
    }

    @Command(name="checkout", mixinStandardHelpOptions = true, description = "Switch branches and update the working directory")
    void checkout(@Parameters(index="0", paramLabel="<branch>", description="branch to checkout") String branch) throws Exception {
        if(!isInitialized())
        {
            System.out.println("store not initialized!!");
            return;
        }
        System.out.println("Proceeding to checkout to branch: "+branch);
        Checkout.checkout(branch);
    }

    @Command(name="status", mixinStandardHelpOptions = true, description = "Show untracked / modified / staged / deleted files")
    void status() throws Exception {
        if(!isInitialized())
        {
            System.out.println("store not initialized!!");
            return;
        }
        Status.status();
    }

    @Command(name="merge", mixinStandardHelpOptions = true, description = "Merge a branch into the current one (never produces conflicts)")
    void merge(@Parameters(index="0", paramLabel="<branch>", description="branch to merge into the current branch") String branch) throws Exception {
        if(!isInitialized())
        {
            System.out.println("store not initialized!!");
            return;
        }
        System.out.println("Proceeding to merge branch: "+branch);
        Merge.merge(branch);
    }

    @Command(name="diff", mixinStandardHelpOptions = true, description = "Line-by-line diff of a file's HEAD version vs. its working directory content")
    void diff(@Parameters(index="0", paramLabel="<file>", description="file to diff against HEAD") String file) throws Exception {
        if(!isInitialized())
        {
            System.out.println("store not initialized!!");
            return;
        }
        Diff.diff(file);
    }

    @Command(name="config", mixinStandardHelpOptions = true, description = "View or set author info and the remote server URL")
    void config(@Option(names="--name", paramLabel="<name>", description="author name") String name,
                @Option(names="--email", paramLabel="<email>", description="author email") String email,
                @Option(names="--remote-url", paramLabel="<url>", description="remote server base URL, e.g. http://localhost:8080") String remoteUrl) throws Exception {
        if(!isInitialized())
        {
            System.out.println("store not initialized!!");
            return;
        }
        if(name == null && email == null && remoteUrl == null)
        {
            Config.showConfig();
        }
        else
        {
            if(name != null)
            {
                Config.setName(name);
            }
            if(email != null)
            {
                Config.setEmail(email);
            }
            if(remoteUrl != null)
            {
                Config.setRemoteUrl(remoteUrl);
            }
        }
    }

    @Command(name="push", mixinStandardHelpOptions = true, description = "Push the current branch to the configured remote")
    void push() throws Exception
    {
        if(!isInitialized())
        {
            System.out.println("store not initialized!!");
            return;
        }
        Push.push();
    }

    @Command(name="pull", mixinStandardHelpOptions = true, description = "Pull the current branch from the configured remote")
    void pull() throws Exception
    {
        if(!isInitialized())
        {
            System.out.println("store not initialized!!");
            return;
        }
        Pull.pull();
    }

    private static void addAllFiles(File directory) throws Exception
    {
        for(Path path : FileUtil.collectTrackableFiles(directory))
        {
            Add.addFile(path);
        }
    }

    private static boolean isInitialized() {
        return Files.exists(Path.of(Constants.root));
    }
}
