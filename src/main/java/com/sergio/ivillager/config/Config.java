package com.sergio.ivillager.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Config {

    public static final Logger LOGGER = LogManager.getLogger(Config.class);

    //FINISHED: The config file needs to be read locally and stored persistently, including ssotoken
    // and expiration time judgment, accesstoken and accesskey storage and judgment, worldview background information
    // FINISHED: all villager entities and their character information should be stored in the world
    //  save data but not config

    public static ForgeConfigSpec COMMON_CONFIG;
    public static ForgeConfigSpec.ConfigValue<String> SOCRATES_EMAIL;
    public static ForgeConfigSpec.ConfigValue<String> SOCRATES_PWD;
    public static ForgeConfigSpec.ConfigValue<String> OPENAI_API_KEY;
    public static ForgeConfigSpec.ConfigValue<String> PROMPT_SERVER_URL;

    public static ForgeConfigSpec.ConfigValue<Boolean> IS_REPLACING_ALL_VILLAGERS;

    public static ForgeConfigSpec.ConfigValue<List<String>> ACTION_TO_EMOJI;

    public static Map<String, String> ActionToEmoji;

    public static ForgeConfigSpec.ConfigValue<Boolean> ENABLE_VILLAGER_RANDOM_CHAT;
    public static ForgeConfigSpec.ConfigValue<String> ENV;

    static {
        ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
        COMMON_BUILDER.comment("General settings").push("general");
        SOCRATES_EMAIL = COMMON_BUILDER.comment("Socrates User Name (email address)").define("EMAIL"
                , "");
        SOCRATES_PWD = COMMON_BUILDER.comment("Socrates User Password ").define("PWD"
                , "");
        OPENAI_API_KEY = COMMON_BUILDER.comment("OpenAI API key ").define("OPENAI_API_KEY"
                , "");
        PROMPT_SERVER_URL = COMMON_BUILDER.comment("Prompt server url ").define("PROMPT_SERVER_URL"
                , "");
        IS_REPLACING_ALL_VILLAGERS =
                COMMON_BUILDER.comment("Replacing all the original villagers or keep both").define(
                "IS_REPLACING_ALL_VILLAGERS"
                , true);
        List<String> defaultEmoji = new ArrayList<>();
        defaultEmoji.add("think=🤔");
        defaultEmoji.add("cry=😂");
        ACTION_TO_EMOJI =
                COMMON_BUILDER.comment("Action to emoji mapping").define("ACTION_TO_EMOJI", defaultEmoji);


        ENABLE_VILLAGER_RANDOM_CHAT = COMMON_BUILDER.comment("Whether villagers can chat with each other").define("ENABLE_VILLAGER_RANDOM_CHAT", false);
        ENV = COMMON_BUILDER.comment("Service runtime environment, optional: dev, prod").define("ENV", "dev");

        COMMON_BUILDER.pop();
        COMMON_CONFIG = COMMON_BUILDER.build();

        ActionToEmoji = new HashMap<>();
        for (String item : ACTION_TO_EMOJI.get()) {
            String[] kv = item.split("=");
            ActionToEmoji.put(kv[0], kv[1]);
        }
    }

}
