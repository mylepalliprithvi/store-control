package push;

import branch.Ref;
import commit.CommitGraph;
import common.Constants;
import config.Config;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

public class Push {

    public static void push() throws Exception {
        Path currentBranchRefPath = Ref.getCurrentBranchRefPath();
        String branchName = currentBranchRefPath.getFileName().toString();

        if(Files.notExists(currentBranchRefPath))
        {
            System.out.println("Nothing to push - branch '"+branchName+"' has no commits yet");
            return;
        }
        String localCommitHash = Files.readString(currentBranchRefPath).trim();

        Path trackingRefPath = Path.of(Constants.remoteOriginPath+"/"+branchName);
        String previousKnownRemoteHash = Files.exists(trackingRefPath) ? Files.readString(trackingRefPath).trim() : null;

        if(localCommitHash.equals(previousKnownRemoteHash))
        {
            System.out.println("Everything up to date");
            return;
        }

        Map<String,byte[]> objects = CommitGraph.collectObjects(localCommitHash,previousKnownRemoteHash);

        String remoteUrl = Config.getRemoteUrl();
        String url = remoteUrl+"/origin/push/"+branchName+"/"+localCommitHash;

        JSONObject objectsJson = new JSONObject();
        for(Map.Entry<String,byte[]> entry : objects.entrySet())
        {
            objectsJson.put(entry.getKey(),Base64.getEncoder().encodeToString(entry.getValue()));
        }
        JSONObject requestBody = new JSONObject();
        requestBody.put("branchName",branchName);
        requestBody.put("commitHash",localCommitHash);
        requestBody.put("objects",objectsJson);

        System.out.println("Pushing "+objects.size()+" object(s) to "+url);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
        HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() != 200)
        {
            System.out.println("Push failed: HTTP "+response.statusCode()+" - "+response.body());
            return;
        }

        JSONObject responseJson = new JSONObject(response.body());
        boolean success = responseJson.optBoolean("success",false);
        String message = responseJson.optString("message","");

        if(!success)
        {
            System.out.println("Push rejected: "+message);
            return;
        }

        String newCommitHash = responseJson.optString("newCommitHash",localCommitHash);
        Files.createDirectories(Path.of(Constants.remoteOriginPath));
        Files.writeString(trackingRefPath,newCommitHash);

        System.out.println("Push accepted: "+message);
        System.out.println("Updated refs/remotes/origin/"+branchName+" -> "+newCommitHash);
    }
}
