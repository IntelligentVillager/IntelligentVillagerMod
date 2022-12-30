package com.sergio.ivillager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.ParseResults;
import com.sergio.ivillager.config.Config;
import com.sergio.ivillager.goal.NPCVillagerLookRandomlyGoal;
import com.sergio.ivillager.goal.NPCVillagerTalkGoal;
import com.sergio.ivillager.goal.NPCVillagerWalkingGoal;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.merchant.villager.VillagerData;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.villager.VillagerType;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.*;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.network.FMLPlayMessages;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.network.datasync.DataSerializers;

import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.World;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.DamageSource;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Item;

import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;

import net.minecraft.entity.merchant.villager.VillagerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.ServerChatEvent;

import java.util.*;

@NPCModElement.ModElement.Tag
@Mod.EventBusSubscriber
public class NPCVillager extends NPCModElement.ModElement {

    public static final Logger LOGGER = LogManager.getLogger(NPCVillager.class);

    public static EntityType entity = (EntityType.Builder.<NPCVillagerEntity>of(NPCVillagerEntity::new,
                    EntityClassification.MISC)
            .setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(NPCVillagerEntity::new)
            .sized(0.6f, 1.95f)).build("ainpc").setRegistryName("ainpc");

    public NPCVillager(NPCModElement instance) {
        super(instance, 1);
        FMLJavaModLoadingContext.get().getModEventBus().register(new EntityAttributesRegisterHandler());
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void initElements() {
        elements.entities.add(() -> entity);
        elements.items.add(
                () -> new SpawnEggItem(entity, -1, -1,
                        new Item.Properties().tab(ItemGroup.TAB_DECORATIONS)).setRegistryName(
                        "test_ainpc_spawn_egg"));
    }

    @SubscribeEvent
    public void addFeatureToBiomes(BiomeLoadingEvent event) {
//        LOGGER.warn("BiomeLoadingEvent");
    }

    @SubscribeEvent
    public static void onLivingVisibilityEvent(LivingEvent.LivingVisibilityEvent event) {
        Entity entity = event.getEntityLiving();
        if (entity instanceof NPCVillagerEntity) {
            NPCVillagerManager.getInstance().addVillager((NPCVillagerEntity) entity);
        }
        if (entity instanceof PlayerEntity) {
            NPCVillagerManager.getInstance().updateVillagers((PlayerEntity) entity);
        }
    }

    @SubscribeEvent
    public static void mobEvent(LivingSpawnEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof NPCVillagerEntity) {
            NPCVillagerManager.getInstance().addVillager((NPCVillagerEntity) entity);
        }
        if (entity instanceof VillagerEntity) {
            NPCVillagerEntity customVillager = (NPCVillagerEntity) NPCVillager.entity.create((World) event.getWorld());
            // set the custom villager's position to the village center
            customVillager.setPos(entity.position().x + 1, entity.position().y,
                    entity.position().z + 1);
            // add the custom villager to the world
            event.getWorld().addFreshEntity(customVillager);

            // add the custom villager to singelton manager
            NPCVillagerManager.getInstance().addVillager(customVillager);

            if (Config.IS_REPLACING_ALL_VILLAGERS.get()) {
                // remove the original villager
                entity.remove();
            }
        }
    }

    @SubscribeEvent
    public void onCommandEvent(CommandEvent event) throws  Exception {
        LOGGER.warn("[SERVER] Received command event");
        LOGGER.info(event.getParseResults());
        ParseResults<CommandSource> result = event.getParseResults();
        Entity sender = result.getContext().getSource().getEntity();
        if (sender instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) sender;
//            String command = result.getContext().getCommand();
        }
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) throws Exception {
        LOGGER.warn(String.format("[SERVER] Received ServerChatEvent from Player: %s", event.getPlayer().getName().getString()));

        ServerPlayerEntity player = event.getPlayer();
        String userMsg  = event.getMessage().toString();
        if (userMsg.startsWith("<villager command>")) {
            event.setCanceled(true);
            userMsg = userMsg.replace("<villager command>", "");
        } else {
            // No need to process normal chat without a custom villager as target
            return;
        }

        JsonObject userMsgJsonObject = Utils.JsonConverter.encodeStringToJson(userMsg);

        // Retrieve target villager UUID list and the original message
        JsonArray jsonArray = userMsgJsonObject.get("interacted").getAsJsonArray();
        String originalMsg = userMsgJsonObject.get("msg").getAsString();

        ITextComponent msg0 = new StringTextComponent(String.format("<%s> %s", player.getName().getString(), originalMsg));
        player.sendMessage(msg0, player.getUUID());
        broadcastToOtherPlayerInInteractiveRange(player, msg0);

        // Build Iterator
        Iterator<JsonElement> iterator = jsonArray.iterator();

        // For every interacted villager, broadcast every response, client need to process the
        // message and choose which to accept and which to abandon
        while(iterator.hasNext()) {
            String villagerUUID = Utils.JsonConverter.encodeStringToJson(iterator);
            NPCVillagerEntity obj = NPCVillagerManager.getInstance().getEntityByUUID(villagerUUID);

            obj.setIsTalkingToPlayer(player);
            obj.goalSelector.disableControlFlag(Goal.Flag.MOVE);
            obj.setProcessingMessage(true);

            interactWithEntity(player, originalMsg, obj);
        }
    }

    private static void broadcastToOtherPlayerInInteractiveRange(ServerPlayerEntity player, ITextComponent msg) {
        List<ServerPlayerEntity> s0 = player.server.getPlayerList().getPlayers();
        for (ServerPlayerEntity obj : s0) {
            if (obj.getId() != player.getId()) {
                Vector3d playerPos = player.position();
                Vector3d villagerPos = obj.position();
                Vector3d playerToVillager = villagerPos.subtract(playerPos);
                double distance = playerToVillager.length();
                if (distance <= 6.0) {
                    obj.sendMessage(msg, obj.getUUID());
                }
            }
        }
    }

    private static void interactWithEntity(ServerPlayerEntity player, String originalMsg, NPCVillagerEntity obj) {
        if (obj != null) {
            NetworkRequestManager.asyncInteractWithNode(player.getUUID(), Utils.TEST_NODE_ID,
                    originalMsg,
                    response -> {
                        Map<String, Object> j0 = new HashMap<String, Object>();
                        j0.put("from_entity", obj.getId());
                        j0.put("to_entity", player.getId());
                        if (response != null) {
                            j0.put("code", 200);
                            j0.put("msg", response.trim());
                        } else {
                            j0.put("code", 0);
                            j0.put("msg", Utils.ERROR_MESSAGE);
                        }

                        obj.getLookControl().setLookAt(player.position());
                        if (originalMsg.startsWith("*") && originalMsg.endsWith("*")) {
                            obj.eatAndDigestFood();
                            obj.setJumping(true);
                        }
                        obj.playTalkSound();
                        obj.setProcessingMessage(false);

                        ITextComponent msg = new StringTextComponent(String.format("<villager" +
                                " response>%s", Utils.JsonConverter.encodeMapToJsonString(j0)));
                        player.sendMessage(msg, player.getUUID());
                        broadcastToOtherPlayerInInteractiveRange(player, msg);
                    });
        }
    }

    @Override
    public void init(FMLCommonSetupEvent event) {

        EntitySpawnPlacementRegistry.register(entity,
                EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                MobEntity::checkMobSpawnRules);
        EntitySpawnPlacementRegistry.register(NPCVillagerBaseEntity.entity,
                EntitySpawnPlacementRegistry.PlacementType.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                MobEntity::checkMobSpawnRules);
    }

    private static class EntityAttributesRegisterHandler {
        @SubscribeEvent
        public void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
            AttributeModifierMap.MutableAttribute ammma = MobEntity.createMobAttributes();
            ammma = ammma.add(Attributes.MOVEMENT_SPEED, 0.4);
            ammma = ammma.add(Attributes.MAX_HEALTH, 10);
            ammma = ammma.add(Attributes.ARMOR, 0);
            ammma = ammma.add(Attributes.ATTACK_DAMAGE, 3);
            ammma = ammma.add(Attributes.FOLLOW_RANGE, 16);
            event.put(entity, ammma.build());
        }
    }

    public static class NPCVillagerEntity extends NPCVillagerBaseEntity {

        // FINISHED: Rename the entity to NPCVillagerEntity

        private PlayerEntity isTalkingToPlayer = null;
        private static final DataParameter<String> CUSTOM_SKIN =
                EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);

        private static final DataParameter<String> CUSTOM_BACKGROUND_INFO =
                EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);

        private static final DataParameter<String> CUSTOM_NODE_ID =
                EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);

        private static final DataParameter<String> CUSTOM_NODE_PUBLIC_ID =
                EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);

        private static final DataParameter<String> CUSTOM_PROFESSION =
                EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);

        private static final DataParameter<String> CUSTOM_VILLAGENAME = EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);

        private static final DataParameter<String> CUSTOM_NAME_COLOR =
                EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);

        // When processing message, villager should look at the player, but after sending the
        // message the villager could continue look randomly
        private Boolean isProcessingMessage = false;

//        private String customSkin = "villager_0";
        private TextFormatting customNameColor = TextFormatting.WHITE;

        public NPCVillagerEntity(FMLPlayMessages.SpawnEntity packet, World world) {
            this(entity, world);
        }

        public NPCVillagerEntity(EntityType<NPCVillagerEntity> type, World world) {
            super(type, world);

            setBaby(false);
            setNoAi(false);

            this.getNavigation().setCanFloat(true);
            this.setCanPickUpLoot(true);

            this.setCustomSkin(Utils.RandomSkinGenerator.generateSkin());

            // TODO: Name and personal background should be generated from GPT3 API instead of
            //  setting it manually
//            setCustomName(ITextComponent.nullToEmpty("Jobbs"));
            setCustomName(new StringTextComponent("Jobbs"));
//            setCustomName(new StringTextComponent(Utils.RandomNameGenerator.generateName()));
            setCustomNameVisible(true);
        }

        protected void defineSynchedData() {
            super.defineSynchedData();
            this.entityData.define(CUSTOM_SKIN,"villager_0");
            this.entityData.define(CUSTOM_BACKGROUND_INFO,"");
            this.entityData.define(CUSTOM_PROFESSION,"");
            this.entityData.define(CUSTOM_VILLAGENAME, "");
            this.entityData.define(CUSTOM_NAME_COLOR, TextFormatting.BLUE.getName());
            this.entityData.define(CUSTOM_NODE_ID, "");
            this.entityData.define(CUSTOM_NODE_PUBLIC_ID, "");
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();


            // TODO: Villager could hear other villagers talking when they are close (Using
            //  NPCVillagerManager) and reply

            // TODO: Villagers need to add a goal to talk to each other, randomly go to the nearby villagers to chat

            // TODO: Players can only receive messages from villagers chatting with each other if they are close to the villagers they are talking to

            this.goalSelector.addGoal(2, new NPCVillagerTalkGoal(this));
            this.goalSelector.addGoal(1, new NPCVillagerLookRandomlyGoal(this));
            this.goalSelector.addGoal(6, new NPCVillagerWalkingGoal(this, 1.0f));
        }

        // FINISHED: Play sound when villager talk

        public void playTalkSound () {
            this.playSound(Utils.RandomAmbientSound.generateSound(), this.getSoundVolume(),
                    this.getVoicePitch());
        }

        @Override
        public net.minecraft.util.SoundEvent getHurtSound(DamageSource ds) {
            return (net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.hurt"));
        }

        @Override
        public net.minecraft.util.SoundEvent getDeathSound() {
            return (net.minecraft.util.SoundEvent) ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.death"));
        }

        public String getCustomSkin() {
            return this.entityData.get(CUSTOM_SKIN);
        }

        public void setCustomSkin(String customSkin) {
            this.entityData.set(CUSTOM_SKIN, customSkin);
        }

        public PlayerEntity getIsTalkingToPlayer() {
            return isTalkingToPlayer;
        }

        public void setIsTalkingToPlayer(PlayerEntity isTalkingToPlayer) {
            this.isTalkingToPlayer = isTalkingToPlayer;
        }

        public Boolean getProcessingMessage() {
            return isProcessingMessage;
        }

        public void setProcessingMessage(Boolean processingMessage) {
            isProcessingMessage = processingMessage;
        }

        public TextFormatting getCustomNameColor() {
            return TextFormatting.getByName(this.entityData.get(CUSTOM_NAME_COLOR));
        }

        public void setCustomNameColor(TextFormatting customNameColor) {
            this.entityData.set(CUSTOM_NAME_COLOR, customNameColor.getName());
        }

        public String getCustomBackgroundInfo() {
            return this.entityData.get(CUSTOM_BACKGROUND_INFO);
        }

        public void setCustomBackgroundInfo(String backgroundInfo) {
            this.entityData.set(CUSTOM_BACKGROUND_INFO, backgroundInfo);
        }

        public String getCustomProfession () {
            return this.entityData.get(CUSTOM_PROFESSION);
        }

        public void setCustomProfession(String profession) {
            this.entityData.set(CUSTOM_PROFESSION, profession);
        }

        public String getCustomVillagename () {
            return this.entityData.get(CUSTOM_VILLAGENAME);
        }

        public void setCustomVillagename(String villageName) {
            this.entityData.set(CUSTOM_VILLAGENAME, villageName);
        }

        public String getCustomNodeId () {
            return this.entityData.get(CUSTOM_NODE_ID);
        }

        public void setCustomNodeId(String n0) {
            this.entityData.set(CUSTOM_NODE_ID, n0);
        }

        public String getCustomNodePublicId () {
            return this.entityData.get(CUSTOM_NODE_PUBLIC_ID);
        }

        public void setCustomNodePublicId(String n0) {
            this.entityData.set(CUSTOM_NODE_PUBLIC_ID, n0);
        }
    }
}
