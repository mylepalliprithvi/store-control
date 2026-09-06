package pull;

import branch.Ref;
import commit.Commit;
import common.Constants;
import config.Config;
import org.json.JSONObject;
import tree.Tree;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

public class Pull {

    public static void pull() throws Exception {
        Path currentBranchRefPath = Ref.getCurrentBranchRefPath();
        String branchName = currentBranchRefPath.getFileName().toString();
        String localCommitHash = Files.exists(currentBranchRefPath) ? Files.readString(currentBranchRefPath).trim() : "";

        String remoteUrl = Config.getRemoteUrl();
        String url = remoteUrl+"/origin/pull/"+branchName;
        if(!localCommitHash.isEmpty())
        {
            url += "?localCommitHash="+URLEncoder.encode(localCommitHash,StandardCharsets.UTF_8);
        }

        System.out.println("Pulling branch '"+branchName+"' from "+url);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() != 200)
        {
            System.out.println("Pull failed: HTTP "+response.statusCode()+" - "+response.body());
            return;
        }

        JSONObject responseJson = new JSONObject(response.body());
        boolean success = responseJson.optBoolean("success",false);
        String message = responseJson.optString("message","");

        if(!success)
        {
            System.out.println("Pull rejected: "+message);
            return;
        }

        String remoteCommitHash = responseJson.optString("commitHash",null);
        if(remoteCommitHash == null || remoteCommitHash.equals(localCommitHash))
        {
            System.out.println(message.isEmpty() ? "Already up to date" : message);
            return;
        }

        JSONObject objectsJson = responseJson.optJSONObject("objects");
        int written = writeNewObjects(objectsJson);
        System.out.println("Wrote "+written+" new object(s)");

        Map<String,String> localBlobPaths = Commit.getBlobPathsForCommit(localCommitHash);
        Map<String,String> remoteBlobPaths = Commit.getBlobPathsForCommit(remoteCommitHash);
        Tree.materializeBlobPaths(remoteBlobPaths,localBlobPaths,true);

        Files.createDirectories(currentBranchRefPath.getParent());
        Files.writeString(currentBranchRefPath,remoteCommitHash);

        Path trackingRefPath = Path.of(Constants.remoteOriginPath+"/"+branchName);
        Files.createDirectories(trackingRefPath.getParent());
        Files.writeString(trackingRefPath,remoteCommitHash);

        System.out.println("Pull complete: "+branchName+" now at "+remoteCommitHash);
    }

    private static int writeNewObjects(JSONObject objectsJson) throws Exception {
        if(objectsJson == null)
        {
            return 0;
        }
        int written = 0;
        for(String hash : objectsJson.keySet())
        {
            Path objPath = Path.of(Constants.root+"/"+Constants.objects,hash.substring(0,2),hash.substring(2));
            if(Files.notExists(objPath))
            {
                Files.createDirectories(objPath.getParent());
                Files.write(objPath,Base64.getDecoder().decode(objectsJson.getString(hash)));
                written++;
            }
        }
        return written;
    }
}
