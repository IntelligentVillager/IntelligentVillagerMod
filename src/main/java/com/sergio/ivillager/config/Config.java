package com.sergio.ivillager.config;
import com.sergio.ivillager.Utils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.io.*;
import java.util.Properties;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;


public class Config {

    //TODO: The config file needs to be read locally and stored persistently, including ssotoken and expiration time judgment, accesstoken and accesskey storage and judgment, worldview background information and all villager entities and their character information
    public static ForgeConfigSpec COMMON_CONFIG;
    public static ForgeConfigSpec.IntValue VALUE;

    private static final String RESOURCE_FILE = new ResourceLocation(Utils.resourcePathBuilder(
            "config", "config.json")).getPath();

    static {
        ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
        COMMON_BUILDER.comment("General settings").push("general");
        VALUE = COMMON_BUILDER.comment("Test config value").defineInRange("value", 10, 0, Integer.MAX_VALUE);
        COMMON_BUILDER.pop();
        COMMON_CONFIG = COMMON_BUILDER.build();

        try {
            InputStream inputStream = new FileInputStream(RESOURCE_FILE);
        // create a JsonReader object from the input stream
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream));
            // create a JsonParser object
            JsonParser parser = new JsonParser();
            // parse the JSON data from the file
            JsonObject data = parser.parse(reader).getAsJsonObject();
            // close the reader
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Properties config;
}
