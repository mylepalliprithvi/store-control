package config;

import common.Constants;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {

    public static void setName(String name) throws IOException {
        JSONObject configJSON = readConfig();
        configJSON.put("authorName",name);
        writeConfig(configJSON);
        System.out.println("Set authorName: "+name);
    }

    public static void setEmail(String email) throws IOException {
        JSONObject configJSON = readConfig();
        configJSON.put("authorEmail",email);
        writeConfig(configJSON);
        System.out.println("Set authorEmail: "+email);
    }

    public static void setRemoteUrl(String remoteUrl) throws IOException {
        JSONObject configJSON = readConfig();
        configJSON.put("remoteUrl",remoteUrl);
        writeConfig(configJSON);
        System.out.println("Set remoteUrl: "+remoteUrl);
    }

    // Defaults to a local Spring Boot instance so this works out of the box
    // for the same-machine client/server setup this project has been tested with.
    public static String getRemoteUrl() throws IOException {
        JSONObject configJSON = readConfig();
        return configJSON.has("remoteUrl") ? configJSON.getString("remoteUrl") : "http://localhost:8080";
    }

    public static void showConfig() throws IOException {
        JSONObject configJSON = readConfig();
        String name = configJSON.has("authorName") ? configJSON.getString("authorName") : "(not set)";
        String email = configJSON.has("authorEmail") ? configJSON.getString("authorEmail") : "(not set)";
        String remoteUrl = configJSON.has("remoteUrl") ? configJSON.getString("remoteUrl") : "(not set, defaults to http://localhost:8080)";
        System.out.println("authorName: "+name);
        System.out.println("authorEmail: "+email);
        System.out.println("remoteUrl: "+remoteUrl);
    }

    private static JSONObject readConfig() throws IOException {
        Path configPath = Path.of(Constants.root+"/"+Constants.configFile);
        String configContents = new String(Files.readAllBytes(configPath));
        return new JSONObject(configContents);
    }

    private static void writeConfig(JSONObject configJSON) throws IOException {
        Path configPath = Path.of(Constants.root+"/"+Constants.configFile);
        Files.writeString(configPath,configJSON.toString());
    }
}
