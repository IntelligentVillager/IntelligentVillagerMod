package com.sergio.ivillager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sergio.ivillager.NPCVillager.NPCVillagerEntity;
import com.sergio.ivillager.config.Config;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;

import java.util.*;

public class Utils {
    public static final String MOD_ID = "intelligentvillager";
    public static final int ENTITYINTELLIGENCE_TICKING_INTERVAL = 1000;
    public static final double CLOSEST_DISTANCE = 7.0F;

    public static final String TEST_NODE_ID = "9dcf5d19-5c4c-4ae0-a75d-56ad27ea892b";

    public static final String ERROR_MESSAGE = "[OOPS] Intelligent Villager Mod seemed not " +
            "functioning " +
            "properly on this server, please contact the server operator for more support.";

    public static final String HINT_MESSAGE_FOR_VILLAGER_CONVERSATION = "Write a line of dialogue" +
            " to open up conversations about %s between two Minecraft villagers: \n";

    public static final String DEFAULT_MESSAGE_FOR_VILLAGER_CONVERSATION = "Hey! How's your day?";

    public static String randomProfession() {
        String[] professions = {"farmer", "librarian", "blacksmith", "carpenter", "herbalist"};
        return professions[new Random().nextInt(professions.length)];
    }

    public static String randomTopic() {
        String[] topics = {"weather", "strange things around the village", "friendship",
                "family", "hobby"};
        return topics[new Random().nextInt(topics.length)];
    }

    public static String randomVillageNameExclude(List<String> excluded) {
        List<String> villages = new ArrayList<>(Arrays.asList("Oakdale", "Meadowvale", "Riverstone",
                "Greenhaven", "Pineville", "Maplewood", "Springfield", "Silverlake", "Sunflower " +
                        "Fields", "Blue Ridge", "Redwood Forest", "Valleyview", "Sunrise Meadows", "Golden Fields", "Rolling Hills", "Forest Glen", "Emberton", "Silver Stream", "Riverwalk", "Autumn Ridge", "Wildflower Plains", "Sunny Acres", "Meadow Brook", "Rustic Ridge", "Blue Moon", "Skyview", "Willow Creek", "Harvest Fields", "Red Rock", "Spring Creek", "Stonehenge", "Green Meadows", "Sunset Hills", "Golden Fields", "Misty Meadows", "Summer Crossing", "Misty Ridge", "Ivy Hill", "Wildflower Meadows", "Meadowview", "New Haven", "Sunflower Fields", "Sunrise Estates", "Emerald Forest", "Woodland Fields", "Meadowland", "Sunny Hill", "Greenfields", "Sunrise Ridge", "Wildflower Meadows", "Rolling Hills", "Stonebridge", "Ivy Hill", "Sunset Meadows", "Wildflower Plains", "Meadow Walk", "Wildflower Fields", "Sunny Hills", "Golden Meadows", "Sunny Ridge", "Wildflower Acres", "Meadowsweet", "Stonebridge", "Autumn Woods", "Misty Meadows", "Wildflower Hill", "Sunny Fields", "Meadow Brook", "Golden Fields", "Wildflower Meadows", "Sunny Acres", "Green Meadows", "Wildflower Hill", "Meadowview", "Riverwalk", "Meadowland", "Meadow Brook", "Meadowview", "Wildflower Hill", "Wildflower Meadows", "Sunny Fields", "Wildflower Plains", "Sunny Hills", "Sunny Ridge", "Meadowland", "Meadow Brook", "Meadowview", "Wildflower Hill", "Wildflower Meadows"));
        if (excluded != null) {
            villages.removeAll(excluded);
        }

        if (villages.size() == 0) {
            villages.add("New Village");
        }

        return villages.get(new Random().nextInt(villages.size()));
    }

    public static String resourcePathBuilder(String file_path, String file_name) {
        return String.format("intelligentvillager:%s" +
                "/%s", file_path, file_name);
    }
    public static String nodeConfigBuilder(String name, String prompt) {
        String p0 = prompt.replaceAll("[^\\x20-\\x7E]", "");
        String s0 = "";
        if (Objects.equals(Config.ENV.get().toLowerCase(), "prod")) {
            s0 = "{\"node_config_id\": 111588, \"models\": [{\"model_id\": 111611,\"default_chat\": " +
                    "{\"default_node\": \"%s\",\"default_node_text\": \"Hey there! What's up!\"," +
                    "\"default_user\": \"user\",\"default_user_text\": \"Hey\"},\"prompts\": " +
                    "[\"%s\"],\"rounds\": 3,\"params\": {\"background\": \"$(background)\"," +
                    "\"default_chat\": \"$(default_chat)\",\"history_dialogue\": \"$" +
                    "(history_dialogue)\",\"engines\": \"text-davinci-002\",\"max_tokens\": 50," +
                    "\"text\": \"$(text)\",\"mode\": \"default\"," +
                    "\"model_language\": 2,\"model_name\": \"GPT3\",\"node_name\": \"$(node_name)\",\"text\":" +
                    " \"$(text)\",\"user_name\": \"$(user_name)\"}}]}";
        } else {
            s0 = "{\"node_config_id\": 111588, \"models\": [{\"model_id\": 111611,\"default_chat\": " +
                    "{\"default_node\": \"%s\",\"default_node_text\": \"Hey there! What's up!\"," +
                    "\"default_user\": \"user\",\"default_user_text\": \"Hey\"},\"prompts\": " +
                    "[\"%s\"],\"rounds\": 3,\"params\": {\"background\": \"$(background)\"," +
                    "\"default_chat\": \"$(default_chat)\",\"history_dialogue\": \"$" +
                    "(history_dialogue)\",\"engines\": \"text-davinci-002\",\"max_tokens\": 50," +
                    "\"text\": \"$(text)\",\"mode\": \"default\"," +
                    "\"model_language\": 2,\"model_name\": \"BLOOM\", \"url\": \"http://8.212.45.202:8033/generate_more_results\",\"node_name\": \"$(node_name)\",\"text\":" +
                    " \"$(text)\",\"user_name\": \"$(user_name)\"}}]}";
        }


        String s1 = String.format(s0, name, "");
        JsonObject bodyMap = JsonConverter.encodeStringToJson(s1);
        JsonArray models = bodyMap.getAsJsonArray("models");
        JsonObject model = models.get(0).getAsJsonObject();
        JsonArray prompts = new JsonArray();
        prompts.add(p0);
        model.add("prompts", prompts);
        models.set(0, model);
        bodyMap.add("models", models);

        return JsonConverter.decodeJsonToString(bodyMap);
    }

    public static class ContextBuilder {

        public static JsonObject build_item_stack(ItemStack itemStack) {
            if (itemStack.isEmpty()) {
                return null;
            }
            JsonObject obj = new JsonObject();
            Item item = itemStack.getItem();
            String name = item.toString();
            int count = itemStack.getCount();
            obj.addProperty("count", count);
            obj.addProperty("name", name);
            obj.addProperty("damage", itemStack.getDamageValue());
            if (itemStack.getEquipmentSlot() != null) {
                obj.addProperty("equip_slot_name", itemStack.getEquipmentSlot().getName());
                obj.addProperty("equip_slot_type", itemStack.getEquipmentSlot().getType().toString());
            }
            obj.addProperty("edible", itemStack.isEdible());
            return obj;
        }

        public static String build_prompt_request_body(Brain<NPCVillagerEntity> brain,
                                                       NPCVillagerEntity contextEntity) {
            Optional<List<LivingEntity>> optional_livingentities =
                    brain.getMemory(MemoryModuleType.LIVING_ENTITIES);
            Optional<List<LivingEntity>> optional_livingentities_visible =
                    brain.getMemory(MemoryModuleType.VISIBLE_LIVING_ENTITIES);
            Optional<List<NPCVillagerEntity>> optional_villagers_in_town =
                    brain.getMemory(NPCVillagerMod.COMPATRIOTS_MEMORY_TYPE);
            Optional<Map<String, Long>> optional_player_attack_history =
                    brain.getMemory(NPCVillagerMod.PLAYER_ATTACK_HISTORY);
            Optional<String> optional_weather_memory =
                    brain.getMemory(NPCVillagerMod.WEATHER_MEMORY);
            Optional<String> optional_golem_protecting =
                    brain.getMemory(NPCVillagerMod.GOLEM_PROTECTING_MEMORY);
            JsonObject req = new JsonObject();
            Gson g = new Gson();

            req.addProperty("name", contextEntity.getName().getString());
            req.addProperty("node_id", contextEntity.getCustomNodeId());
            req.addProperty("background", contextEntity.getCustomBackgroundInfo());

            Map<String, List<String>> livingEntities = new HashMap<>();
            if (optional_livingentities.isPresent()) {
                List<LivingEntity> entities = optional_livingentities.get();
                if (entities.size() > 0) {
                    for (LivingEntity entity : entities) {
                        String type = "villager";
                        if (entity instanceof NPCVillagerEntity) {
                            type = "villager";
                        } else if (entity instanceof PlayerEntity) {
                            type = "player";
                        } else {
                            type = "other";
                        }
                        List<String> villagers = new ArrayList<>();
                        if (livingEntities.containsKey(type)) {
                            villagers = livingEntities.get(type);
                        }
                        villagers.add(entity.getName().getString());
                        livingEntities.put(type, villagers);

                    }
                }
            }
            req.add("living_entities", g.toJsonTree(livingEntities));


            Map<String, List<String>> otherVillagers = new HashMap<>();

            if (optional_villagers_in_town.isPresent()) {
                List<NPCVillagerEntity> entities = optional_villagers_in_town.get();
                if (entities.size() > 0) {
                    for (NPCVillagerEntity entity : entities) {
                        String type = entity.getCustomProfession();
                        List<String> villagers = new ArrayList<>();
                        if (otherVillagers.containsKey(type)) {
                            villagers = otherVillagers.get(type);
                        }
                        villagers.add(entity.getName().getString());
                        otherVillagers.put(type, villagers);
                    }
                }
            }


            req.add("other_villagers", g.toJsonTree(otherVillagers));


            Map<String, Long> history = new HashMap<>();
            if (optional_player_attack_history.isPresent()) {
                Map<String, Long> m0 = optional_player_attack_history.get();
                MinecraftServer s1 = contextEntity.getServer();
                if (s1 != null) {
                    for (String uuid : m0.keySet()) {
                        ServerPlayerEntity p0 = s1.getPlayerList().getPlayer(UUID.fromString(uuid));
                        String name = p0.getName().getString();
                        history.put(name, m0.get(uuid));
                    }
                }
            }
            req.add("player_attack_history", g.toJsonTree(history));
            req.addProperty("village_name", contextEntity.getCustomVillagename());
            req.addProperty("villager_name", contextEntity.getName().getString());

            if (optional_weather_memory.isPresent()) {
                req.addProperty("weather", optional_weather_memory.get());
            }

            if (optional_golem_protecting.isPresent()) {
                req.addProperty("golem_protection", optional_golem_protecting.get());
            }

            JsonArray inventorySlots = new JsonArray();
            if (!contextEntity.getInventory().isEmpty()) {
                for (int i = 0; i < contextEntity.getInventory().getContainerSize(); i++) {
                    ItemStack itemStack = contextEntity.getInventory().getItem(i);
                    if (!itemStack.isEmpty()) {
                        inventorySlots.add(build_item_stack(itemStack));
                    }
                }
            }

            req.add("inventory", inventorySlots);

            ItemStack mainHand = contextEntity.getItemBySlot(EquipmentSlotType.MAINHAND);
            req.add("main_hand", build_item_stack(mainHand));
            req.add("off_hand", build_item_stack(contextEntity.getItemBySlot(EquipmentSlotType.OFFHAND)));
            req.add("feet", build_item_stack(contextEntity.getItemBySlot(EquipmentSlotType.FEET)));
            req.add("legs", build_item_stack(contextEntity.getItemBySlot(EquipmentSlotType.LEGS)));
            req.add("chest", build_item_stack(contextEntity.getItemBySlot(EquipmentSlotType.CHEST)));
            req.add("head", build_item_stack(contextEntity.getItemBySlot(EquipmentSlotType.HEAD)));

            JsonArray availableActions = new JsonArray();
            for (String action : Config.ACTION_TO_EMOJI.get()) {
                availableActions.add(action);
            }

            String validActions[] = new String[] {
                    "friendly pat", "wave hand", "wave hands", "punch", "beat", "equip", "attack with", "attack",
                    "eat", "give", "give away"
            };
            for (String action : validActions) {
                availableActions.add(action);
            }

            req.add("available_actions", availableActions);


            return g.toJson(req);
        }
    }

    public static class RandomSkinGenerator {
        private static final String[] IMAGE_NAMES = {
                "villager_0", "villager_1", "villager_2",
                "villager_3", "villager_4", "villager_5"
        };
        ;

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
        };

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

}
