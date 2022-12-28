package com.sergio.ivillager.config;
import com.sergio.ivillager.Utils;
import com.sergio.ivillager.goal.NPCVillagerLookRandomlyGoal;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.io.*;
import java.util.Properties;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Config {

    public static final Logger LOGGER = LogManager.getLogger(Config.class);

    //FINISHED: The config file needs to be read locally and stored persistently, including ssotoken
    // and expiration time judgment, accesstoken and accesskey storage and judgment, worldview background information
    // TODO: all villager entities and their character information should be stored in the world
    //  save data but not config

    public static ForgeConfigSpec COMMON_CONFIG;
    public static ForgeConfigSpec.ConfigValue<String> SOCRATES_EMAIL;
    public static ForgeConfigSpec.ConfigValue<String> SOCRATES_PWD;

    static {
        ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
        COMMON_BUILDER.comment("General settings").push("general");
        SOCRATES_EMAIL = COMMON_BUILDER.comment("Socrates User Name (email address)").define("EMAIL"
                , "");
        SOCRATES_PWD = COMMON_BUILDER.comment("Socrates User Password ").define("PWD"
                , "");

        COMMON_BUILDER.pop();
        COMMON_CONFIG = COMMON_BUILDER.build();
    }

}
