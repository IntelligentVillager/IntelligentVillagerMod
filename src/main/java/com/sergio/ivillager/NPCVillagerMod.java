package com.sergio.ivillager;

import com.sergio.ivillager.NPCVillager.NPCVillagerEntity;
import com.sergio.ivillager.ai.NPCVillagerCompatriotsSensor;
import com.sergio.ivillager.config.Config;
import com.sergio.ivillager.renderer.NPCVillagerRenderer;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
    public static final MemoryModuleType<List<NPCVillagerEntity>> COMPATRIOTS_MEMORY_TYPE =
            new MemoryModuleType<List<NPCVillagerEntity>>(Optional.empty());

    public static final MemoryModuleType<Map<String, Long>> PLAYER_ATTACK_HISTORY =
            new MemoryModuleType<Map<String, Long>>(Optional.empty());

    public static final SensorType<NPCVillagerCompatriotsSensor> COMPATRIOTS_SENSOR_TYPE =
            new SensorType<>(NPCVillagerCompatriotsSensor::new);

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

        MinecraftForge.EVENT_BUS.register(new ClientChatInject());
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

    @SubscribeEvent
    public void registerMemoryModuleType(RegistryEvent.Register<MemoryModuleType<?>> event) {
        LOGGER.info("register mod memory module type");
        COMPATRIOTS_MEMORY_TYPE.setRegistryName(Utils.MOD_ID, "compatriots_memory_type");
        PLAYER_ATTACK_HISTORY.setRegistryName(Utils.MOD_ID, "attacked_by_player_memory_type");
        event.getRegistry().register(PLAYER_ATTACK_HISTORY);
        event.getRegistry().register(COMPATRIOTS_MEMORY_TYPE);
    }

    @SubscribeEvent
    public void registerSensorType(RegistryEvent.Register<SensorType<?>> event) {
        LOGGER.info("register mod sensor type");
        COMPATRIOTS_SENSOR_TYPE.setRegistryName(Utils.MOD_ID, "compatriots_sensor_type");
        event.getRegistry().register(COMPATRIOTS_SENSOR_TYPE);
    }

    private static class NPCVillagerModFMLBusEvents {
        private final NPCVillagerMod parent;

        NPCVillagerModFMLBusEvents(NPCVillagerMod parent) {
            this.parent = parent;
        }

        @SubscribeEvent
        public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            LOGGER.info(String.format("[%s] Player %s has logged out.",
                    event.getPlayer().getStringUUID(),
                    event.getPlayer().getName().getString()));
        }

        @SubscribeEvent
        public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {

//            LOGGER.info(String.format("[%s] Player %s has logged in.",
//                    event.getPlayer().getStringUUID(),
//                    event.getPlayer().getName().getString()));
            if (NPCVillagerManager.getInstance().getSsoToken() != null) {
                Map<String, String> accessKeyandToken =
                        NetworkRequestManager.getAccessToken(NPCVillagerManager.getInstance().getSsoToken());

                NPCVillagerManager.getInstance().setAccessKey(event.getPlayer().getUUID(), accessKeyandToken.get("key"));
                NPCVillagerManager.getInstance().setAccessToken(event.getPlayer().getUUID(),
                        accessKeyandToken.get("token"));

                LOGGER.warn(String.format("[%s] Successfully generated Socrates ACCESS KEY:%s and \n " +
                                "ACCESS TOKEN:%s for Player %s",
                        event.getPlayer().getStringUUID(), accessKeyandToken.get(
                                "key"), accessKeyandToken.get("token"), event.getPlayer().getName().getString()));
            }
        }

        @SubscribeEvent
        public void serverLoad(FMLServerStartingEvent event) throws Exception {
            LOGGER.warn("serverLoad");

            // FINISHED: email and password read from config file
            // FINISHED: server integration debugging (last step)

            String socrates_username = Config.SOCRATES_EMAIL.get().toString();
            String socrates_userpwd = Config.SOCRATES_PWD.get().toString();
            String openAPAPIKey = Config.OPENAI_API_KEY.get().toString();
            NPCVillagerManager.getInstance().setOpenAIAPIKey(openAPAPIKey);

            if (socrates_username.equals("") || socrates_userpwd.equals("")) {
                LOGGER.error("IMPORTANT! SET YOUR SOCRATES USER AUTHENTICATION IN THE" +
                        " CONFIG FILE " +
                        "UNDER MINECRAFT FOLDER TO INITIATE INTELLIGENTVILLAGER MOD!");
            } else {
                String ssotoken = NetworkRequestManager.getAuthToken(socrates_username, socrates_userpwd);
                LOGGER.info(String.format("Socrates sso token: %s", ssotoken));
                NPCVillagerManager.getInstance().setSsoToken(ssotoken);
            }

            // FINISHED: access_token and key contains history messages and context, it should be
            //  stored locally with the worldSaveData

            this.parent.elements.getElements().forEach(element -> element.serverLoad(event));
        }
    }


}
