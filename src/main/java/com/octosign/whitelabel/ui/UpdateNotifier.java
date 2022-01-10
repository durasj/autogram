package com.octosign.whitelabel.ui;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static com.octosign.whitelabel.ui.FXUtils.displayInfo;
import static com.octosign.whitelabel.ui.I18n.translate;

public class UpdateNotifier {
    private static final String RELEASES_URL = env("GITHUB_API_URL") + "/repos/" + env("GITHUB_REPOSITORY") + "/releases";

    public static void checkForUpdates() {
        //Update disabled in DEV environment
        if (currentVersion().equals("dev")) {
            return;
        }

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder().uri(new URI(RELEASES_URL + "/latest"))
                                 .header("Accept", "application/vnd.github.v3+json")
                                 .GET().build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException ignored) {
            return;
        }

        JsonElement tagName = new Gson().fromJson(response.body(), JsonObject.class).get("tag_name");

        if (tagName == null) return;
         String latestVersion = tagName.getAsString();

        if (!latestVersion.equalsIgnoreCase(currentVersion())) {
            displayInfo("info.updateAvailable.header", translate("info.updateAvailable.description", latestVersion, currentVersion(), RELEASES_URL));
        }
    }

    private static String env(String key) {
        return System.getenv().get(key);
    }

    private static String currentVersion() {
        return Main.getVersion();
    }

}