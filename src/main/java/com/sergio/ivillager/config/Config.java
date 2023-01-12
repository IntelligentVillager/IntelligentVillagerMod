package com.sergio.ivillager.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


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

        COMMON_BUILDER.pop();
        COMMON_CONFIG = COMMON_BUILDER.build();
    }

}
