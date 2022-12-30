package com.sergio.ivillager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;

import java.util.Map;

public class Utils {
    public static final String MOD_ID = "intelligentvillager";

    public static final String TEST_NODE_ID = "9dcf5d19-5c4c-4ae0-a75d-56ad27ea892b";

    public static final String ERROR_MESSAGE = "[OOPS] Intelligent Villager Mod seemed not " +
            "functioning " +
            "properly on this server, please contact the server operator for more support.";

    public static class RandomNameGenerator {

        // TODO: this method should be removed once GPT3 API completed
        private static final String[] FIRST_NAMES = {
                "Alice", "Bob", "Charlie", "Dave", "Eve", "Frank", "Gina", "Harry", "Ivy", "Jack", "Kate", "Linda", "Mike", "Nina", "Oscar", "Peggy", "Quincy", "Rita", "Sam", "Tina", "Ursula", "Violet", "Wendy", "Xander", "Yolanda", "Zach"
        };

        private static final String[] LAST_NAMES = {
                "Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller", "Wilson", "Moore", "Taylor", "Anderson", "Thomas", "Jackson", "White", "Harris", "Martin", "Thompson", "Garcia", "Martinez", "Robinson", "Clark", "Rodriguez", "Lewis", "Lee", "Walker", "Hall", "Allen", "Young", "King", "Wright", "Scott", "Green", "Baker", "Adams", "Nelson", "Carter", "Mitchell", "Perez", "Roberts", "Turner", "Phillips", "Campbell", "Parker", "Evans", "Edwards", "Collins", "Stewart", "Sanchez", "Morris", "Rogers", "Reed", "Cook", "Morgan", "Bell", "Murphy", "Bailey", "Rivera", "Cooper", "Richardson", "Cox", "Howard", "Ward", "Torres", "Peterson", "Gray", "Ramirez", "James", "Watson", "Brooks", "Kelly", "Sanders", "Price", "Bennett", "Wood", "Barnes", "Ross", "Henderson", "Coleman", "Jenkins", "Perry", "Powell", "Long", "Patterson", "Hughes", "Flores", "Washington", "Butler", "Simmons", "Foster", "Gonzales", "Bryant", "Alexander", "Russell", "Griffin", "Diaz", "Hayes"
        };

        public static String generateName() {
            Random random = new Random();
            int firstIndex = random.nextInt(FIRST_NAMES.length);
            int lastIndex = random.nextInt(LAST_NAMES.length);
            return FIRST_NAMES[firstIndex] + " " + LAST_NAMES[lastIndex];
        }
    }

    public static class RandomSkinGenerator {
        private static final String[] IMAGE_NAMES = {
                "villager_0", "villager_1", "villager_2",
                "villager_3", "villager_4", "villager_5"
        };;

        public static String generateSkin() {
            Random rand = new Random();
            int index = rand.nextInt(IMAGE_NAMES.length);
            return IMAGE_NAMES[index];
        }
    }

    public static class RandomAmbientSound {
        private static final SoundEvent[] Sound_NAMES = {
                SoundEvents.VILLAGER_AMBIENT, SoundEvents.VILLAGER_CELEBRATE,
                SoundEvents.VILLAGER_YES, SoundEvents.VILLAGER_NO
        };;

        public static SoundEvent generateSound() {
            Random rand = new Random();
            int index = rand.nextInt(Sound_NAMES.length);
            return Sound_NAMES[index];
        }
    }

    public static class JsonConverter {
        private static Gson gson = new Gson();

        public static JsonObject encodeMapToJson(Map<String, Object> map) {
            String jsonString = gson.toJson(map);
            return gson.fromJson(jsonString, JsonObject.class);
        }

        public static String encodeMapToJsonString(Map<String, Object> map) {
            return gson.toJson(map);
        }

        public static String decodeJsonToString(JsonObject jsonObject) {
            return gson.toJson(jsonObject);
        }

        public static JsonObject encodeStringToJson(String jsonString) {
            return gson.fromJson(jsonString, JsonObject.class);
        }

        public static String encodeStringToJson(Iterator<JsonElement> iterator) {
            return gson.fromJson(iterator.next(), String.class);
        }

    }

    public static String resourcePathBuilder(String file_path, String file_name)
    {
        return String.format("intelligentvillager:%s" +
                "/%s", file_path, file_name);
    }

    public static String nodeConfigBuilder(String name, String prompt){
        String s0 = "{\n" +
                "  \"node_config_id\": 207,\n" +
                "  \"models\": [\n" +
                "    {\n" +
                "      \"model_id\": 206,\n" +
                "      \"default_chat\": {\n" +
                "        \"default_node\": \"%s\",\n" +
                "        \"default_node_text\": \"Hey there! What's up!\",\n" +
                "        \"default_user\": \"user\",\n" +
                "        \"default_user_text\": \"Hey\"\n" +
                "      },\n" +
                "      \"prompts\": [\n" +
                "       \"%s\"" +
                "      ],\n" +
                "      \"rounds\": 3,\n" +
                "      \"params\": {\n" +
                "        \"background\": \"$(background)\",\n" +
                "        \"default_chat\": \"$(default_chat)\",\n" +
                "        \"history_dialogue\": \"$(history_dialogue)\",\n" +
                "        \"max_tokens\": 50,\n" +
                "        \"node_emotion\": \"$(node_emotional)\",\n" +
                "        \"prompt\": \"$(prompt)\",\n" +
                "        \"strategy\": \"append\",\n" +
                "        \"style\": \"arrogant\",\n" +
                "        \"text\": \"$(text)\",\n" +
                "        \"user_name\": \"$(user_name)\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]";
        return String.format(s0, name, prompt);
    }

}
