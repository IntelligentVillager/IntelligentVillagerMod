package com.sergio.ivillager;

import com.google.gson.JsonObject;
import com.sergio.ivillager.Utils.JsonConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class NetworkRequestManager {


    // TODO: create Node API integration
    // TODO: set prompt API integration
    // TODO: GPT3 API integration

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

    public static String getAuthToken(String email, String password) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("email", email);
            data.put("password", password);

            String result = NetworkRequestManager.sendPostRequest(URLs.AUTH_URL.getUrl(), String.format("{\"email\":\"%s\", \"password\":\"%s\"}", email, password));
            return JsonConverter.encodeStringToJson(result).getAsJsonObject("data").get("ssotoken").getAsString();
        } catch (Exception e) {
            LOGGER.error(e);
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public static Map<String, String> getAccessToken(String ssoToken)
    {
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

    public static CompletableFuture<Void> asyncInteractWithNode(String nodeId, String text, Consumer<String> callback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!NPCVillagerManager
                        .getInstance().isVerified()){
                    // authentication information not set in config yet
                    return null;
                }

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
                LOGGER.error(e);
                e.printStackTrace();
                return e.getMessage();
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


    public static String randomProfession() {
        String[] professions = {"farmer", "librarian", "blacksmith", "carpenter", "herbalist"};
        // Create a Random instance
        Random random = new Random();

        // Choose a random element from each array
        int professionIndex = random.nextInt(professions.length);
        String profession = professions[professionIndex];
        return profession;
    }

    public static String randomVillageName() {
        String[] villages = {"Oakdale", "Meadowvale", "Riverstone", "Greenhaven", "Pineville", "Maplewood", "Springfield", "Silverlake", "Sunflower Fields", "Blue Ridge", "Redwood Forest", "Valleyview", "Sunrise Meadows", "Golden Fields", "Rolling Hills", "Forest Glen", "Emberton", "Silver Stream", "Riverwalk", "Autumn Ridge", "Wildflower Plains", "Sunny Acres", "Meadow Brook", "Rustic Ridge", "Blue Moon", "Skyview", "Willow Creek", "Harvest Fields", "Red Rock", "Spring Creek", "Stonehenge", "Green Meadows", "Sunset Hills", "Golden Fields", "Misty Meadows", "Summer Crossing", "Misty Ridge", "Ivy Hill", "Wildflower Meadows", "Meadowview", "New Haven", "Sunflower Fields", "Sunrise Estates", "Emerald Forest", "Woodland Fields", "Meadowland", "Sunny Hill", "Greenfields", "Sunrise Ridge", "Wildflower Meadows", "Rolling Hills", "Stonebridge", "Ivy Hill", "Sunset Meadows", "Wildflower Plains", "Meadow Walk", "Wildflower Fields", "Sunny Hills", "Golden Meadows", "Sunny Ridge", "Wildflower Acres", "Meadowsweet", "Stonebridge", "Autumn Woods", "Misty Meadows", "Wildflower Hill", "Sunny Fields", "Meadow Brook", "Golden Fields", "Wildflower Meadows", "Sunny Acres", "Green Meadows", "Wildflower Hill", "Meadowview", "Riverwalk", "Meadowland", "Meadow Brook", "Meadowview", "Wildflower Hill", "Wildflower Meadows", "Sunny Fields", "Wildflower Plains", "Sunny Hills", "Sunny Ridge", "Meadowland", "Meadow Brook", "Meadowview", "Wildflower Hill", "Wildflower Meadows"};
        // Create a Random instance
        Random random = new Random();

        // Choose a random element from each array
        int villageIndex = random.nextInt(villages.length);
        String village = villages[villageIndex];
        return village;
    }

    public static String[] generateVillager(String APIkey, String villageName, String profession) {
        try {
            // Set up the HTTP connection
            URL url = new URL("https://api.openai.com/v1/completions");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Authorization", String.format("Bearer %s", APIkey));

            // Set the request body

            String[] prompts = {
                    "Name and background story for a %s living in %s",
                    "Name and background story for a %s living in %s, who has a unique hobby or pastime",
                    "Name and background story for a %s living in %s, who has a tragic or difficult past",
                    "Name and background story for a %s living in %s, who is the leader of their profession",
                    "Name and background story for a %s living in %s, who has a strong relationship with another villager in the village",
                    "Name and background story for a %s living in %s, who has a hidden talent or secret",
                    "Name and background story for a %s living in %s, who has a unique appearance or physical characteristic",
                    // 以下是 Minecraft 专属
                    "Name and background story for a %s NPC living in a %s village in the Minecraft world",
                    "Name and background story for a %s NPC living in a %s village in the Minecraft world, who has an interesting or unique relationship with a player character",
                    "Name and background story for a %s NPC living in a %s village in the Minecraft world, who has a special role or importance in their village",
                    "Name and background story for a %s NPC living in a %s village in the Minecraft world, who has a memorable or noteworthy encounter with a player character",
            };

            Random random = new Random();
            // Choose a random element from the array
            int index = random.nextInt(prompts.length);
            String selected = prompts[index];

            // Format the selected element with the given string
            String prompt = String.format(selected, profession, villageName);

            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("prompt", prompt);
            bodyMap.put("max_tokens", 1024);
            bodyMap.put("model", "text-davinci-003");
            bodyMap.put("temperature", 0.7);
            bodyMap.put("top_p", 1);
            bodyMap.put("frequency_penalty", 0);
            bodyMap.put("presence_penalty", 0);

            String body = JsonConverter.decodeJsonToString(JsonConverter.encodeMapToJson(bodyMap));

//            String body = "{\"prompt\":\"" + prompt + ":\",\"max_tokens\":1024,\"temperature\":0.5}";
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(body);
            wr.flush();
            wr.close();

            // Send the request and get the response
            int responseCode = con.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Parse the response as a JSON object
            String story = JsonConverter.encodeStringToJson(response.toString()).getAsJsonArray("choices").get(0).getAsJsonObject().get("text").getAsString();

            // Split the story into a name and background
            String[] parts = story.split("\n", 2);
            String name = parts[0];
            name = name.replace("Name: ", "");
            String background = parts[1];
            background = background.replace("Background Story: ", "");

            System.out.println("Name: " + name);
            System.out.println("Background: " + background);
            parts = new String[]{name, background};
            return parts;

        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] result = {"", ""};
        return result;
    }
}

