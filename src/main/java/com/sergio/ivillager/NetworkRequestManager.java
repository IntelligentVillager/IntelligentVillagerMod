package com.sergio.ivillager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sergio.ivillager.Utils.JsonConverter;
import com.sergio.ivillager.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class NetworkRequestManager {

    public static final Logger LOGGER = LogManager.getLogger(NetworkRequestManager.class);
    private static final String USER_AGENT = "Mozilla/5.0";

    public static Executor executor = Executors.newFixedThreadPool(50);

    public static String getAuthToken(String email, String password) {
        try {
            String result = NetworkRequestManager.sendPostRequest(URLs.AUTH_URL.getUrl(), String.format("{\"email\":\"%s\", \"password\":\"%s\"}", email, password));
            return JsonConverter.encodeStringToJson(result).getAsJsonObject("data").get("ssotoken").getAsString();
        } catch (Exception e) {
            LOGGER.error(e);
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public static String[] createNodeId(String name, String ssoToken) {
        String[] result = {"", ""};
        if (ssoToken == null) {
            return result;
        }
        try {
            Map<String, String> data = new HashMap<>();
            data.put("x-sso-token", ssoToken);

            String resultStr =
                    NetworkRequestManager.sendPostRequestWithHeader(URLs.CREATE_NODE_URL.getUrl()
                            , data, String.format("{\"name\":\"%s\", \"introduce\":\"%s\"," +
                                    "\"level\":\"L2\"}", name, "This is an auto-generated node."));
            JsonObject resultJson =
                    JsonConverter.encodeStringToJson(resultStr);
            if (0 == resultJson.get("code").getAsInt()) {
                result[0] = resultJson.getAsJsonObject("data").get("public_id").getAsString();
                result[1] = resultJson.getAsJsonObject("data").get("id").getAsString();
            }
            return result;
        } catch (Exception e) {
            LOGGER.error(e);
            e.printStackTrace();
            return result;
        }
    }
    public static Boolean setNodePrompt(String name, String ssoToken, String nodeId,
                                     String backgroundInfo) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("x-sso-token", ssoToken);

            String body = Utils.nodeConfigBuilder(name, backgroundInfo);

            String resultStr =
                    NetworkRequestManager.sendPutRequestWithHeader(String.format(
                                    URLs.SET_NODE_URL.getUrl(), nodeId)
                            , data, body);
            JsonObject resultJson =
                    JsonConverter.encodeStringToJson(resultStr);
            if (0 == resultJson.get("code").getAsInt()) {
                return true;
            } else {
                LOGGER.error(resultStr);
                return false;
            }
        } catch (Exception e) {
            LOGGER.error(e);
            e.printStackTrace();
            return false;
        }
    }

    public static Map<String, String> generateIntelligence(String villageName, String profession, String openAIKey, String ssoToken) {
        Map<String, String> result = new HashMap<>();
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("x-sso-token", ssoToken);

            JsonObject body = new JsonObject();
            Gson gson = new Gson();
            body.addProperty("village_name", villageName);
            body.addProperty("profession", profession);
            body.addProperty("openai_key", openAIKey);

            String resultStr =
                    NetworkRequestManager.sendPostRequestWithHeader(URLs.GENERATE_INTELLIGENCE_URL.getUrl()
                            , headers, gson.toJson(body));
            JsonObject resultJson =
                    JsonConverter.encodeStringToJson(resultStr);
            int code = resultJson.get("code").getAsInt();
            if (code == 0) {
                result.put("msg", "success");
                result.put("name", resultJson.get("name").getAsString());
                result.put("public_id", resultJson.get("public_id").getAsString());
                result.put("background", resultJson.get("background").getAsString());
                result.put("node_id", resultJson.get("node_id").getAsString());
            } else {
                result.put("msg", "error");
            }
            return result;
        } catch (Exception e) {
            LOGGER.error(e);
            e.printStackTrace();
            result.put("msg", "error");
            return result;
        }
    }

    public static Map<String, String> refreshIntelligence(String req, String ssoToken) {
        Map<String, String> result = new HashMap<>();
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("x-sso-token", ssoToken);

            String resultStr =
                    NetworkRequestManager.sendPostRequestWithHeader(URLs.REFRESH_INTELLIGENCE_URL.getUrl()
                            , headers, req);
            JsonObject resultJson =
                    JsonConverter.encodeStringToJson(resultStr);
            int code = resultJson.get("code").getAsInt();
            if (code == 0) {
                result.put("msg", "success");
                result.put("prompt", resultJson.get("prompt").getAsString());
            } else {
                result.put("msg", "error");
            }
            return result;
        } catch (Exception e) {
            LOGGER.error(e);
            e.printStackTrace();
            result.put("msg", "error");
            return result;
        }
    }

    public static void setNodePrompt(String name, String ssoToken, String nodeId,
                                        String backgroundInfo, Consumer<Boolean> callback) {
        CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> data = new HashMap<>();
                data.put("x-sso-token", ssoToken);

                String body = Utils.nodeConfigBuilder(name, backgroundInfo);

                String resultStr =
                        NetworkRequestManager.sendPutRequestWithHeader(String.format(
                                        URLs.SET_NODE_URL.getUrl(), nodeId)
                                , data, body);
                JsonObject resultJson =
                        JsonConverter.encodeStringToJson(resultStr);
                if (0 == resultJson.get("code").getAsInt()) {
                    return true;
                } else {
                    LOGGER.error(resultStr);
                    return false;
                }
            } catch (Exception e) {
                LOGGER.error(e);
                e.printStackTrace();
                return false;
            }
        }, executor).thenAccept(callback);
    }


    public static void buildPrompt(String req, Consumer<String> callback) {
        CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> data = new HashMap<>();

                String body = req;

                String url = Config.PROMPT_SERVER_URL.get();
                if (Objects.equals(url, "")) {
                    LOGGER.error("prompt server url is empty, please set in config file");
                    return "";
                }
                String resultStr =
                        NetworkRequestManager.sendPostRequestWithHeader(url, data, body);
                JsonObject resultJson =
                        JsonConverter.encodeStringToJson(resultStr);
                return resultJson.get("prompt").getAsString();
            } catch (Exception e) {
                LOGGER.error(e);
                e.printStackTrace();
                return "";
            }
        }, executor).thenAccept(callback);
    }


    public static Map<String, String> getAccessToken(String ssoToken) {
        Map<String, String> result = new HashMap<>();
        result.put("key", null);
        result.put("token", null);

        try {
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
                result.put("key", accessKey);
                result.put("token", accessToken);
            }
            return result;
        } catch (Exception e) {
            LOGGER.error(e);
            e.printStackTrace();
            return result;
        }
    }

    public static void asyncInteractWithNode(String nodeId, String originalMsg,
                                             Consumer<String> callback) {
        CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(Duration.ofSeconds(4).toMillis());

                if (!NPCVillagerManager
                        .getInstance().isVerified()) {
                    // authentication information not set in config yet
                    return null;
                }

                String text = originalMsg;
                if (text == null) {
                    text = generateInitialConversation(Config.OPENAI_API_KEY.get());
                }

                String resultStr =
                        NetworkRequestManager.sendPostRequest(String.format(URLs.INTERACT_URL.getUrl(), nodeId,
                                        NPCVillagerManager
                                                .getInstance()
                                                .getAccessKey_default(),
                                        NPCVillagerManager
                                                .getInstance()
                                                .getAccessToken_default()),
                                String.format("{\"text" +
                                        "\":\"%s\"}", text));
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
                LOGGER.error(e);
                e.printStackTrace();
                return e.getMessage();
            }
        }, executor).thenAccept(callback);
    }

    public static void asyncInteractWithNode(UUID PlayerId, String nodeId, String text, Consumer<String> callback) {
        CompletableFuture.supplyAsync(() -> {
            try {
                if (!NPCVillagerManager
                        .getInstance().isVerified(PlayerId)) {
                    // authentication information not set in config yet
                    return null;
                }

                String resultStr =
                        NetworkRequestManager.sendPostRequest(String.format(URLs.INTERACT_URL.getUrl(), nodeId,
                                        NPCVillagerManager
                                                .getInstance()
                                                .getAccessKey(PlayerId),
                                        NPCVillagerManager
                                                .getInstance()
                                                .getAccessToken(PlayerId)),
                                String.format("{\"text" +
                                        "\":\"%s\"}", text));
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
                LOGGER.error(e);
                e.printStackTrace();
                return e.getMessage();
            }
        }, executor).thenAccept(callback);
    }

    public static String sendPutRequestWithHeader(String url, Map<String, String> payload,
                                                  String body) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("PUT");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("User-Agent", USER_AGENT);
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            con.setRequestProperty(entry.getKey(), entry.getValue());
        }

        LOGGER.debug(String.format("setNodePrompt Payload:%s", body));

        con.setUseCaches(false);
        con.setDoInput(true);
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(body);
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

        return response.toString();
    }

    public static String sendPostRequestWithHeader(String url, Map<String, String> payload, String body) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("User-Agent", USER_AGENT);
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            con.setRequestProperty(entry.getKey(), entry.getValue());
        }

        con.setUseCaches(false);
        con.setDoInput(true);
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(body);
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
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setRequestProperty("User-Agent", USER_AGENT);

        con.setUseCaches(false);
        con.setDoInput(true);
        con.setDoOutput(true);
        OutputStream outputStream = con.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
        writer.write(payload);
        writer.flush();
        writer.close();

        int responseCode = con.getResponseCode();
        LOGGER.info("Response Code : " + responseCode);

        if (responseCode == 405) {
            return "He is awakening";
        } else if (responseCode != 200) {
            return "Something has gone wrong, please try again";
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        con.disconnect();

//        LOGGER.info("Response : " + response.toString());

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


    public static String generateInitialConversation(String APIkey) {
        try {
            // Set up the HTTP connection
            URL url = new URL("https://api.openai.com/v1/completions");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Authorization", String.format("Bearer %s", APIkey));

            String prompt = String.format(Utils.HINT_MESSAGE_FOR_VILLAGER_CONVERSATION,
                    Utils.randomTopic());
            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("prompt", prompt);
            bodyMap.put("max_tokens", 200);
            bodyMap.put("model", "text-davinci-003");
            bodyMap.put("temperature", 0.7);
            bodyMap.put("top_p", 1);
            bodyMap.put("frequency_penalty", 0);
            bodyMap.put("presence_penalty", 0);

            String body = JsonConverter.decodeJsonToString(JsonConverter.encodeMapToJson(bodyMap));
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(body);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            if (responseCode != 200) {
                return Utils.DEFAULT_MESSAGE_FOR_VILLAGER_CONVERSATION;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Parse the response as a JSON object
            String s0 = JsonConverter.encodeStringToJson(response.toString()).getAsJsonArray(
                    "choices").get(0).getAsJsonObject().get("text").getAsString();
            s0 = s0.replace("Villager 1:", "").replace("\n", "").replaceAll("[^\\x20-\\x7E]", "");
            
            return s0;
        } catch (Exception e) {
            e.printStackTrace();
            return Utils.DEFAULT_MESSAGE_FOR_VILLAGER_CONVERSATION;
        }
    }

    public enum URLs {
        AUTH_URL("https://sso-int-api-prod.rct.ai/auth/login"),
        ACCESSTOKEN_URL("https://socrates-api.rct.ai/v1/applications/95878/subusers"),
        CREATE_NODE_URL("https://socrates-api.rct.ai/v1/applications/95878/nodes/full"),
        SET_NODE_URL("https://socrates-api.rct.ai/v1/applications/95878/nodes/%s/node_config"),
        GENERATE_INTELLIGENCE_URL("https://socrates-api.rct.ai/v11/generate_intelligence"),
        REFRESH_INTELLIGENCE_URL("https://socrates-api.rct.ai/v11/refresh_intelligence"),
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
}

