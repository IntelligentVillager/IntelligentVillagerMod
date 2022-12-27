package com.sergio.ivillager;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.sergio.ivillager.Utils.JsonConverter;

public class NetworkRequestManager {

    public enum URLs {
        AUTH_URL("https://sso-int-api-prod.rct.ai/auth/login"),
        ACCESSTOKEN_URL("https://socrates-api.rct.ai/v1/applications/95878/subusers"),
        INTERACT_URL("https://socrates-api.rct.ai/v1/applications/95878/nodes/%s/conversation" +
                "?accessKey=%s&accessToken=%s");

        private final String url;

        URLs(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }
    private static final String USER_AGENT = "Mozilla/5.0";
    public static final Logger LOGGER = LogManager.getLogger(NetworkRequestManager.class);

    public static String getAuthToken(String email, String password) throws Exception {

        Map<String, String> data = new HashMap<>();
        data.put("email", email);
        data.put("password", password);

        String result = NetworkRequestManager.sendPostRequest(URLs.AUTH_URL.getUrl(), String.format("{\"email\":\"%s\", \"password\":\"%s\"}", email, password));
        return JsonConverter.encodeStringToJson(result).getAsJsonObject("data").get("ssotoken").getAsString();
    }

    public static Map<String, String> getAccessToken(String ssoToken) throws Exception {
        Map<String, String> result = new HashMap<>();
        Map<String, String> data = new HashMap<>();
        data.put("x-sso-token", ssoToken);

        String accessKey = "", accessToken = "";

        String resultStr =
                NetworkRequestManager.sendPostRequestWithHeader(URLs.ACCESSTOKEN_URL.getUrl(),
                        data);
        JsonObject resultJson =
                JsonConverter.encodeStringToJson(resultStr);
        if (0 == resultJson.get("code").getAsInt()) {
            JsonObject data_tmp = resultJson.getAsJsonObject("data").getAsJsonObject("subusers");
            accessKey = data_tmp.get("access_key").getAsString();
            accessToken = data_tmp.get("access_token").getAsString();
        }
        result.put("key", accessKey);
        result.put("token", accessToken);
        return result;
    }

    public static String syncInteractWithNode(String nodeId, String text) throws Exception {
        String resultStr =
                NetworkRequestManager.sendPostRequest(String.format(URLs.INTERACT_URL.getUrl(), nodeId,
                                NPCVillagerManager
                                        .getInstance()
                                        .getAccessKey(),
                                NPCVillagerManager
                                        .getInstance()
                                        .getAccessToken()),
                        String.format("{\"text" +
                "\":\"%s\"}",text));
        JsonObject resultJson =
                JsonConverter.encodeStringToJson(resultStr);
        if (0 == resultJson.get("code").getAsInt()) {
            return resultJson.get("data")
                    .getAsJsonArray()
                    .get(0)
                    .getAsJsonObject()
                    .get("text")
                    .getAsString();
        }

        return null;
    }

    public static CompletableFuture<Void> asyncInteractWithNode(String nodeId, String text, Consumer<String> callback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String resultStr =
                        NetworkRequestManager.sendPostRequest(String.format(URLs.INTERACT_URL.getUrl(), nodeId,
                                        NPCVillagerManager
                                                .getInstance()
                                                .getAccessKey(),
                                        NPCVillagerManager
                                                .getInstance()
                                                .getAccessToken()),
                                String.format("{\"text" +
                                        "\":\"%s\"}",text));
                JsonObject resultJson =
                        JsonConverter.encodeStringToJson(resultStr);
                if (0 == resultJson.get("code").getAsInt()) {
                    return resultJson.get("data")
                            .getAsJsonArray()
                            .get(0)
                            .getAsJsonObject()
                            .get("text")
                            .getAsString();
                }
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(callback);
    }

    public static String sendPostRequestWithHeader(String url, Map<String, String> payload) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("POST");
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            con.setRequestProperty(entry.getKey(), entry.getValue());
        }

        con.setUseCaches(false);
        con.setDoInput(true);
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes("");
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        LOGGER.info("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        con.disconnect();

        LOGGER.info("Response : " + response.toString());

        return response.toString();
    }

    public static String sendPostRequest(String url, String payload) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("User-Agent", USER_AGENT);

        con.setUseCaches(false);
        con.setDoInput(true);
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(payload);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        LOGGER.info("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        con.disconnect();

        LOGGER.info("Response : " + response.toString());

        return response.toString();
    }

    public static String sendPutRequest(String url, String payload) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("PUT");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(payload);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        LOGGER.info("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        con.disconnect();

        LOGGER.info("Response : " + response.toString());

        return response.toString();
    }

    public static String sendGetRequest(String url) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("User-Agent", USER_AGENT);

        int responseCode = con.getResponseCode();
        LOGGER.info("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        con.disconnect();

        LOGGER.info("Response : " + response.toString());

        return response.toString();
    }
}

