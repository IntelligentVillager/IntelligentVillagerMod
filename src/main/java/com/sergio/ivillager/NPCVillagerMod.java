package com.sergio.ivillager;

import com.sergio.ivillager.config.Config;
import com.sergio.ivillager.renderer.NPCVillagerRenderer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.util.ResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.entity.EntityType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.block.Block;

import java.util.Map;
import java.util.function.Supplier;


@Mod(Utils.MOD_ID)
@Mod.EventBusSubscriber
public class NPCVillagerMod {
    public static final Logger LOGGER = LogManager.getLogger(NPCVillagerMod.class);
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel PACKET_HANDLER =
            NetworkRegistry.newSimpleChannel(new ResourceLocation(Utils.MOD_ID, Utils.MOD_ID),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    public NPCModElement elements;

    public NPCVillagerMod() {
        elements = new NPCModElement();

        LOGGER.info("IntelligentVillager mod loading");

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_CONFIG);

        FMLJavaModLoadingContext.get().getModEventBus().register(this);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientLoad);
        MinecraftForge.EVENT_BUS.register(new NPCVillagerModFMLBusEvents(this));
    }

    private void init(FMLCommonSetupEvent event) {
        elements.getElements().forEach(element -> element.init(event));
    }

    public void clientLoad(FMLClientSetupEvent event) {
        LOGGER.warn("clientLoad");

        RenderingRegistry.registerEntityRenderingHandler(NPCVillager.entity, NPCVillagerRenderer::new);

        elements.getElements().forEach(element -> element.clientLoad(event));
    }

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(elements.getBlocks().stream().map(Supplier::get).toArray(Block[]::new));
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(elements.getItems().stream().map(Supplier::get).toArray(Item[]::new));
    }

    @SubscribeEvent
    public void registerEntities(RegistryEvent.Register<EntityType<?>> event) {
        event.getRegistry().registerAll(elements.getEntities().stream().map(Supplier::get).toArray(EntityType[]::new));
    }

    @SubscribeEvent
    public void registerEnchantments(RegistryEvent.Register<Enchantment> event) {
        event.getRegistry().registerAll(elements.getEnchantments().stream().map(Supplier::get).toArray(Enchantment[]::new));
    }

    @SubscribeEvent
    public void registerSounds(RegistryEvent.Register<net.minecraft.util.SoundEvent> event) {
        elements.registerSounds(event);
    }

    private static class NPCVillagerModFMLBusEvents {
        private final NPCVillagerMod parent;

        NPCVillagerModFMLBusEvents(NPCVillagerMod parent) {
            this.parent = parent;
        }

        @SubscribeEvent
        public void serverLoad(FMLServerStartingEvent event) throws Exception {
            LOGGER.warn("serverLoad");

            // FINISHED: email and password read from config file
            // TODO: server integration debugging (last step)

            String socrates_username = Config.SOCRATES_EMAIL.get().toString();
            String socrates_userpwd = Config.SOCRATES_PWD.get().toString();

            if (socrates_username.equals("") || socrates_userpwd.equals("")) {
                LOGGER.error("IMPORTANT! SET YOUR SOCRATES USER AUTHENTICATION IN THE" +
                        " CONFIG FILE " +
                        "UNDER MINECRAFT FOLDER TO INITIATE INTELLIGENTVILLAGER MOD!");
            } else {
                String ssotoken = NetworkRequestManager.getAuthToken(socrates_username, socrates_userpwd);
                LOGGER.info(String.format("Socrates sso token: %s", ssotoken));
                NPCVillagerManager.getInstance().setSsoToken(ssotoken);

                Map<String, String> accessKeyandToken = NetworkRequestManager.getAccessToken(ssotoken);
                NPCVillagerManager.getInstance().setAccessKey(accessKeyandToken.get("key"));
                NPCVillagerManager.getInstance().setAccessToken(accessKeyandToken.get("token"));
                LOGGER.warn(String.format("ACCESS KEY:%s \n ACCESS TOKEN:%s", accessKeyandToken.get(
                        "key"),accessKeyandToken.get("token")));
            }

            // TODO: access_token and key contains history messages and context, it should be
            //  stored locally with the worldSaveData

            this.parent.elements.getElements().forEach(element -> element.serverLoad(event));
        }
    }


}
