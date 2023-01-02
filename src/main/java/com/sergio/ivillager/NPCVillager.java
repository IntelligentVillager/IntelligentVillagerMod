package com.sergio.ivillager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.ParseResults;
import com.mojang.serialization.Dynamic;
import com.sergio.ivillager.config.Config;
import com.sergio.ivillager.goal.NPCVillagerLookRandomlyGoal;
import com.sergio.ivillager.goal.NPCVillagerTalkGoal;
import com.sergio.ivillager.goal.NPCVillagerWalkingGoal;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.*;
import net.minecraft.village.PointOfInterestType;
import net.minecraft.world.gen.feature.structure.VillageStructure;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.event.world.StructureSpawnListGatherEvent;
import net.minecraft.world.gen.feature.structure.Structure;

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
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

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
    public void structureLoading(StructureSpawnListGatherEvent event) {
        Structure<?> village = event.getStructure();
        if (village instanceof VillageStructure) {
            LOGGER.warn("[SERVER] Generating Village structure");
        }
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

            if (customVillager != null) {
                customVillager.setPos(entity.position().x + 1, entity.position().y,
                            entity.position().z + 1);

                if (customVillager.getCustomVillagename().equals("")) {
                    String n0 =
                            NPCVillagerManager.getInstance().isEntityLocatedAtVillagesWithName(customVillager,
                                    new BlockPos(entity.position().x + 1, entity.position().y, entity.position().z + 1));
                    if (n0 != null) {
                        customVillager.setCustomVillagename(n0);
                    }
                }

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
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) throws Exception {
        if (event.getEntityLiving() instanceof ServerPlayerEntity) {
            if  (event.getTarget() instanceof NPCVillagerEntity) {
                NPCVillagerEntity e0 = (NPCVillagerEntity) event.getTarget();
                interactWithEntityWithAction((ServerPlayerEntity) event.getEntityLiving(),
                        "(friendly pat)", e0);
            }
        }
    }

    @SubscribeEvent
    public void onLivingAttackEvent(LivingAttackEvent event) throws Exception {
        if (event.getEntityLiving() instanceof NPCVillagerEntity) {
            NPCVillagerEntity e0 = (NPCVillagerEntity) event.getEntityLiving();
            if (event.getSource().msgId.equals("player")){
                if (event.getSource().getEntity() instanceof ServerPlayerEntity) {
                    LOGGER.warn(String.format("[SERVER] Villager %s is being attacked by: %s",
                            e0.getName().getString(),
                            event.getSource().getEntity().getName().getString()));

                    Long current_time_stamp = System.currentTimeMillis();
                    Brain<NPCVillagerEntity> b0 = e0.getBrain();
                    Optional<Map<String, Long>> optional =
                            b0.getMemory(NPCVillagerMod.PLAYER_ATTACK_HISTORY);
                    if (optional.isPresent()) {
                        Map<String, Long> m0 = optional.get();
                        m0.put(event.getSource().getEntity().getStringUUID(), current_time_stamp);
                        b0.setMemory(NPCVillagerMod.PLAYER_ATTACK_HISTORY, m0);
                    } else {
                        Map<String, Long> m0 = new HashMap<>();
                        m0.put(event.getSource().getEntity().getStringUUID(), current_time_stamp);
                        b0.setMemory(NPCVillagerMod.PLAYER_ATTACK_HISTORY, m0);
                    }
                    interactWithEntityWithAction((ServerPlayerEntity) event.getSource().getEntity(), "(punch)", e0);
                }
            }
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

    private static void interactWithEntityWithAction(ServerPlayerEntity player, String actionMsg,
                                            NPCVillagerEntity obj) {
        if (obj != null) {
            NetworkRequestManager.asyncInteractWithNode(player.getUUID(), obj.getCustomNodePublicId(),
                    actionMsg,
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

                        ITextComponent msg = new StringTextComponent(String.format("<villager" +
                                " response>%s", Utils.JsonConverter.encodeMapToJsonString(j0)));
                        player.sendMessage(msg, player.getUUID());
                        broadcastToOtherPlayerInInteractiveRange(player, msg);
                    });
        }
    }

    private static void interactWithEntity(ServerPlayerEntity player, String originalMsg, NPCVillagerEntity obj) {
        if (obj != null) {
            NetworkRequestManager.asyncInteractWithNode(player.getUUID(), obj.getCustomNodePublicId(),
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
            LOGGER.info("[SERVER] Build attribute for villagers");
            AttributeModifierMap.MutableAttribute ammma = MobEntity.createMobAttributes();
            ammma = ammma.add(Attributes.MOVEMENT_SPEED, 0.4);
            ammma = ammma.add(Attributes.MAX_HEALTH, 1000);
            ammma = ammma.add(Attributes.ARMOR, 50);
            ammma = ammma.add(Attributes.ATTACK_DAMAGE, 3);
            ammma = ammma.add(Attributes.FOLLOW_RANGE, 16);
            event.put(entity, ammma.build());
        }
    }

    public static class NPCVillagerEntity extends NPCVillagerBaseEntity {
        private PlayerEntity isTalkingToPlayer = null;
        private static final DataParameter<String> CUSTOM_SKIN = EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);
        private static final DataParameter<String> CUSTOM_BACKGROUND_INFO = EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);
        private static final DataParameter<String> CUSTOM_CONTEXT_INFO =
                EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);

        private static final DataParameter<String> CUSTOM_NODE_ID = EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);
        private static final DataParameter<String> CUSTOM_NODE_PUBLIC_ID = EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);
        private static final DataParameter<String> CUSTOM_PROFESSION = EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);
        private static final DataParameter<String> CUSTOM_VILLAGENAME = EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);
        private static final DataParameter<String> CUSTOM_NAME_COLOR = EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);
        private static final DataParameter<Boolean> HAS_AWAKEN =
                EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.BOOLEAN);

        // When processing message, villager should look at the player, but after sending the
        // message the villager could continue look randomly
        private Boolean isProcessingMessage = false;
        private TextFormatting customNameColor = TextFormatting.WHITE;

        // Refresh intelligence with latest context every 100 ticks by default
        private int remainingRefreshIntelligenceTick = Utils.ENTITYINTELLIGENCE_TICKING_INTERVAL;

        private static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES =
                ImmutableList.of(MemoryModuleType.LIVING_ENTITIES,
                        MemoryModuleType.VISIBLE_LIVING_ENTITIES,
                        MemoryModuleType.NEAREST_PLAYERS, MemoryModuleType.NEAREST_VISIBLE_PLAYER
                        , MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER,
                        MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryModuleType.HURT_BY,
                        MemoryModuleType.HURT_BY_ENTITY, MemoryModuleType.NEAREST_HOSTILE,
                        MemoryModuleType.HEARD_BELL_TIME,
                        MemoryModuleType.GOLEM_DETECTED_RECENTLY,
                        NPCVillagerMod.COMPATRIOTS_MEMORY_TYPE, NPCVillagerMod.PLAYER_ATTACK_HISTORY);

        private static final ImmutableList<SensorType<? extends Sensor<? super NPCVillagerEntity>>> SENSOR_TYPES
                = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS
                , SensorType.NEAREST_ITEMS, SensorType.HURT_BY, SensorType.VILLAGER_HOSTILES,
                SensorType.GOLEM_DETECTED, NPCVillagerMod.COMPATRIOTS_SENSOR_TYPE);

        public NPCVillagerEntity(FMLPlayMessages.SpawnEntity packet, World world) {
            this(entity, world);
        }

        public NPCVillagerEntity(EntityType<NPCVillagerEntity> type, World world) {
            super(type, world);

            setBaby(false);
            setNoAi(false);

            this.getNavigation().setCanFloat(true);
            this.setCanPickUpLoot(true);
            this.setCustomSkin(this.getCustomSkin());
            setCustomName(new StringTextComponent("[Awakening...]"));
            setCustomNameVisible(true);
        }

        public Brain<NPCVillagerEntity> getBrain() {
            return (Brain<NPCVillagerEntity>)super.getBrain();
        }

        protected Brain.BrainCodec<NPCVillagerEntity> brainProvider() {
            return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
        }

        protected Brain<?> makeBrain(Dynamic<?> p_213364_1_) {
            return this.brainProvider().makeBrain(p_213364_1_);
        }

        public void refreshBrain(ServerWorld p_213770_1_) {
            Brain<NPCVillagerEntity> brain = this.getBrain();
            brain.stopAll(p_213770_1_, this);
            this.brain = brain.copyWithoutBehaviors();
        }

        protected void defineSynchedData() {
            super.defineSynchedData();
            this.entityData.define(CUSTOM_SKIN,Utils.RandomSkinGenerator.generateSkin());
            this.entityData.define(CUSTOM_BACKGROUND_INFO,"");
            this.entityData.define(CUSTOM_PROFESSION,Utils.randomProfession());
            this.entityData.define(CUSTOM_VILLAGENAME, "");
            this.entityData.define(CUSTOM_NAME_COLOR, TextFormatting.BLUE.getName());
            this.entityData.define(CUSTOM_NODE_ID, "");
            this.entityData.define(CUSTOM_NODE_PUBLIC_ID, "");
            this.entityData.define(CUSTOM_CONTEXT_INFO, "");
            this.entityData.define(HAS_AWAKEN, false);
        }

        protected void customServerAiStep() {
            this.level.getProfiler().push("villagerBrain");
            this.getBrain().tick((ServerWorld)this.level, this);
            this.level.getProfiler().pop();

            super.customServerAiStep();
        }

        public void tick() {
            super.tick();
            if (remainingRefreshIntelligenceTick > 0) {
                --remainingRefreshIntelligenceTick;
            } else {
                this.refreshIntelligence();
                remainingRefreshIntelligenceTick = Utils.ENTITYINTELLIGENCE_TICKING_INTERVAL;
            }
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

        //region GETTER/SETTER for all custom data
        public Boolean getHasAwaken() {
            return this.entityData.get(HAS_AWAKEN);
        }

        public void setHasAwaken(Boolean flag) {
            this.entityData.set(HAS_AWAKEN, flag);
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

        public String getCustomContext () {
            return this.entityData.get(CUSTOM_CONTEXT_INFO);
        }

        public void setCustomContext(String n0) {
            this.entityData.set(CUSTOM_CONTEXT_INFO, n0);
        }
        //endregion

        public void readAdditionalSaveData(CompoundNBT p_70037_1_) {
            super.readAdditionalSaveData(p_70037_1_);
            NPCVillager.LOGGER.info(String.format("[SERVER] [%s] LOAD Villager additional data",
                    this.getStringUUID()));

            if (!p_70037_1_.getString("s_skin").equals("")) {
                this.setCustomSkin(p_70037_1_.getString("s_skin"));
            } else {
                this.setCustomSkin(Utils.RandomSkinGenerator.generateSkin());
            }

            if (!p_70037_1_.getString("s_name").equals("")) {
                this.setCustomName(new StringTextComponent(p_70037_1_.getString("s_name")));
            }

            this.setCustomBackgroundInfo(p_70037_1_.getString("s_background"));

            TextFormatting t0 = TextFormatting.getByName(p_70037_1_.getString("s_namecolor"));
            if (t0 != null){
                this.setCustomNameColor(t0);
            }

            this.setCustomVillagename(p_70037_1_.getString("s_villagename"));
            this.setCustomNodeId(p_70037_1_.getString("s_nodeId"));
            this.setCustomNodePublicId(p_70037_1_.getString("s_nodePublicId"));
            this.setCustomProfession(p_70037_1_.getString("s_profession"));
            this.setCustomContext(p_70037_1_.getString("s_context"));
            this.setHasAwaken(p_70037_1_.getBoolean("s_hasAwaken"));
        }

        public void addAdditionalSaveData(CompoundNBT p_213281_1_) {
            super.addAdditionalSaveData(p_213281_1_);
            NPCVillager.LOGGER.info(String.format("[SERVER] [%s] SAVE Villager additional data",
                    this.getStringUUID()));
            if (this.getCustomName()!= null) {
                p_213281_1_.putString("s_name", this.getCustomName().getString());
            } else {
                p_213281_1_.putString("s_name", "[Awakening...]");
            }
            p_213281_1_.putString("s_villagename",this.getCustomVillagename());
            p_213281_1_.putString("s_profession",this.getCustomProfession());
            p_213281_1_.putString("s_background",this.getCustomBackgroundInfo());
            p_213281_1_.putString("s_skin",this.getCustomSkin());
            p_213281_1_.putString("s_namecolor",this.getCustomNameColor().getName());
            p_213281_1_.putString("s_nodeId",this.getCustomNodeId());
            p_213281_1_.putString("s_nodePublicId",this.getCustomNodePublicId());
            p_213281_1_.putString("s_context", this.getCustomContext());
            p_213281_1_.putBoolean("s_hasAwaken", this.getHasAwaken());
        }

        public void refreshIntelligence(){
            if (!this.getHasAwaken()) {return;}
            String c0 = Utils.ContextBuilder.build(this.getBrain(), this);
            this.setCustomContext(c0);

            String ssotoken = NPCVillagerManager.getInstance().getSsoToken();
            if ((ssotoken == null) || ssotoken.equals("")) {
                return;
            }

            NetworkRequestManager.setNodePrompt(this.getName().getString(), ssotoken,
                    this.getCustomNodeId(), String.format("%s\n%s",this.getCustomBackgroundInfo()
                            , this.getCustomContext()));
        }

        public void generateIntelligence(){
            this.asyncGenerateIntelligence(response -> {
                if (response != null) {
                    this.setCustomName(new StringTextComponent(response.get("s_name")));
                    this.setCustomNodeId(response.get("s_nodeId"));
                    this.setCustomNodePublicId(response.get("s_nodePublicId"));
                    this.setCustomBackgroundInfo(response.get("s_background"));
                    this.setHasAwaken(true);
                } else {
                    // Generate failed
                    this.remove();
                }
            });
        }

        public void asyncGenerateIntelligence(Consumer<Map<String, String>> callback) {
            CompletableFuture.supplyAsync(() -> {
                try {
                    String ssotoken = NPCVillagerManager.getInstance().getSsoToken();
                    if ((ssotoken == null) || ssotoken.equals("")) {
                        LOGGER.error("IMPORTANT! SET YOUR SOCRATES USER AUTHENTICATION IN THE" +
                                " CONFIG FILE " +
                                "UNDER MINECRAFT FOLDER TO INITIATE INTELLIGENTVILLAGER MOD!");
                        return null;
                    }

                    String customVillagename = this.getCustomVillagename();
                    String customProfession = this.getCustomProfession();
                    String[] p0 =
                            NetworkRequestManager.generateVillager(Config.OPENAI_API_KEY.get(),
                                    customVillagename, customProfession);
                    if (p0[0].equals("") || p0[1].equals("")) {
                        LOGGER.error("[SERVER] Generate character name and background fail, check" +
                                " error message");
                        return null;
                    }

                    String customName = p0[0];
                    String customBackground = p0[1];

                    String[] p1 = NetworkRequestManager.createNodeId(customName, ssotoken);
                    if (p1[0].equals("") || p1[1].equals("")) {
                        LOGGER.error("[SERVER] Generate character node fail, check" +
                                " error message");
                        return null;
                    }

                    String nodePublicId = p1[0];
                    String nodeId = p1[1];

                    Boolean flag = NetworkRequestManager.setNodePrompt(customName, ssotoken,
                            nodeId, customBackground);
                    if (flag) {
                        //Successfully create and set node
                        Map<String, String> result = new HashMap<>();
                        result.put("s_name", customName);
                        result.put("s_background", customBackground);
                        result.put("s_nodeId", nodeId);
                        result.put("s_nodePublicId", nodePublicId);
                        return result;
                    }
                    return null;
                } catch (Exception e) {
                    LOGGER.error(e);
                    e.printStackTrace();
                    return null;
                }
            }).thenAccept(callback);
        }
    }
}
