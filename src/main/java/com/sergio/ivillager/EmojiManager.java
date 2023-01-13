package com.sergio.ivillager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class EmojiManager {

    private final JsonObject config;

    private static EmojiManager instance;
    public static EmojiManager getInstance() {
        if (instance == null) {
            instance = new EmojiManager();
        }
        return instance;
    }

    public EmojiManager() {
        JsonObject config1;
        Gson gson = new Gson();
        try {
            InputStream inputStream = Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation("intelligentvillager:emoji.json")).getInputStream();
            Reader reader = new InputStreamReader(inputStream);
            config1 = gson.fromJson(reader, JsonObject.class);
        } catch (IOException ignored) {
            config1 = new JsonObject();
        }
        this.config = config1;
    }

    public ResourceLocation getTextureByEmoji(String emoji) {
        if (this.config.has(emoji)) {
            return new ResourceLocation(String.format("intelligentvillager:textures/emoji/%s", this.config.get(emoji).getAsString()));
        }
        return null;
    }
}
