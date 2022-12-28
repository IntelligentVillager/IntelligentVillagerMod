package com.sergio.ivillager.config;
import net.minecraftforge.common.ForgeConfigSpec;


public class Config {

    //TODO: The config file needs to be read locally and stored persistently, including ssotoken and expiration time judgment, accesstoken and accesskey storage and judgment, worldview background information and all villager entities and their character information
    public static ForgeConfigSpec COMMON_CONFIG;
    public static ForgeConfigSpec.IntValue VALUE;

    static {
        ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
        COMMON_BUILDER.comment("General settings").push("general");
        VALUE = COMMON_BUILDER.comment("Test config value").defineInRange("value", 10, 0, Integer.MAX_VALUE);
        COMMON_BUILDER.pop();
        COMMON_CONFIG = COMMON_BUILDER.build();
    }
}
