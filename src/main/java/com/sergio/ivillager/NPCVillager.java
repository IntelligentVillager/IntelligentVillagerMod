package com.sergio.ivillager;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.ParseResults;
import com.mojang.serialization.Dynamic;
import com.sergio.ivillager.config.Config;
import com.sergio.ivillager.goal.NPCVillagerLookRandomlyGoal;
import com.sergio.ivillager.goal.NPCVillagerRandomChatGoal;
import com.sergio.ivillager.goal.NPCVillagerTalkGoal;
import com.sergio.ivillager.goal.NPCVillagerWalkingGoal;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.VillageStructure;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.event.world.StructureSpawnListGatherEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLPlayMessages;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                customVillager.setPos(entity.position().x, entity.position().y,
                        entity.position().z);

                if (customVillager.getCustomVillagename().equals("")) {
                    String n0 =
                            NPCVillagerManager.getInstance().isEntityLocatedAtVillagesWithName(customVillager,
                                    new BlockPos(entity.position().x, entity.position().y, entity.position().z));
                    if (n0 != null) {
                        customVillager.setCustomVillagename(n0);
                    }
                }

                if (NPCVillagerManager.getInstance().findVillagersAtSameVillage(customVillager.getCustomVillagename()).size()<5) {
                    // add the custom villager to the world
                    event.getWorld().addFreshEntity(customVillager);
                    // add the custom villager to singelton manager
                    NPCVillagerManager.getInstance().addVillager(customVillager);
                }

                if (Config.IS_REPLACING_ALL_VILLAGERS.get()) {
                    // remove the original villager
                    entity.remove();
                }
            }
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
                if (distance <= Utils.CLOSEST_DISTANCE) {
                    obj.sendMessage(msg, obj.getUUID());
                }
            }
        }
    }

    private static void broadcastToOtherPlayerInInteractiveRange(NPCVillagerEntity entity,
                                                                 ITextComponent msg) {
        List<ServerPlayerEntity> s0 = entity.level.getServer().getPlayerList().getPlayers();
        for (ServerPlayerEntity obj : s0) {
            Vector3d playerPos = obj.position();
            Vector3d villagerPos = entity.position();
            Vector3d playerToVillager = playerPos.subtract(villagerPos);
            double distance = playerToVillager.length();
            if (distance <= Utils.CLOSEST_DISTANCE) {
                obj.sendMessage(msg, obj.getUUID());
            }
        }
    }

    private static void interactWithEntityWithAction(ServerPlayerEntity player, String actionMsg,
                                                     NPCVillagerEntity obj) {
        if (obj != null) {
            obj.setIsTalkingToPlayer(player);
            obj.setProcessingMessage(true);

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

                        if (response != null) {
                            respondToPotentialActionResponse(player, response.trim(), obj);
                        }

                        obj.setProcessingMessage(false);

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

                        if (response != null) {
                            respondToPotentialActionResponse(player, response.trim(), obj);
                        }

                        obj.setProcessingMessage(false);

                        ITextComponent msg = new StringTextComponent(String.format("<villager" +
                                " response>%s", Utils.JsonConverter.encodeMapToJsonString(j0)));
                        player.sendMessage(msg, player.getUUID());
                        broadcastToOtherPlayerInInteractiveRange(player, msg);
                    });
        }
    }

    private static void respondToPotentialActionResponse(ServerPlayerEntity player, String originalMsg,
                                                         NPCVillagerEntity f0) {
        f0.getLookControl().setLookAt(player.position());
        processActionAndTag(originalMsg, f0);
        respondToPotentialActionResponse(originalMsg, player, f0);
    }

    private static void respondToPotentialActionResponse(String originalMsg, ServerPlayerEntity toEntity, NPCVillagerEntity f0) {
        Pattern pattern1 = Pattern.compile("\\((.*?)\\)");

        Matcher macher = pattern1.matcher(originalMsg);
        while (macher.find()) {
            String action = macher.group(1).toLowerCase();
            Vector3d villagerPos = f0.position();
            Vector3d playerPos = toEntity.position();
            Vector3d playerToVillager = villagerPos.subtract(playerPos);
            double distance = playerToVillager.length();
            switch (action) {
                case "friendly pat":
                    f0.waveHands(Hand.MAIN_HAND, false);
                    break;
                case "wave hand":
                case "wave hands":
                    f0.waveHands(Hand.MAIN_HAND, false);
                    f0.waveHands(Hand.OFF_HAND, false);
                    break;
                case "punch":
                case "beat":
                    f0.waveHands(Hand.MAIN_HAND,true);
                    f0.getLookControl().setLookAt(toEntity
                            .position()
                            .add(0.0f, 1.0f, 0.0f));
                    if (distance <= 6.0f) {
                        toEntity.hurt(DamageSource.mobAttack(f0), 0.5f);  // server side effects
                    }
                    break;
            }
            if (action.startsWith("equip")) {
                String target = action.replace("equips", "").replace("equip", "").trim();
                List<Class<?>> clses = new ArrayList<>();
                switch (target) {
                    case "weapon":
                        clses.add(AxeItem.class);
                        clses.add(SwordItem.class);
                        break;
                    case "armor":
                        clses.add(ArmorItem.class);
                        break;
                    case "sword":
                        clses.add(SwordItem.class);
                        break;
                    case "axe":
                        clses.add(AxeItem.class);
                        break;
                }
                if (!f0.getInventory().isEmpty()) {
                    if (!clses.isEmpty()) {
                        for (int i = 0; i < f0.getInventory().getContainerSize(); i++) {
                            ItemStack itemStack = f0.getInventory().getItem(i);
                            if (!itemStack.isEmpty()) {
                                boolean equipped = false;
                                Item item = itemStack.getItem();
                                for (Class<?> cls : clses) {
                                    if (cls.isInstance(item)) {
                                        f0.equipItemIfPossible(itemStack);
                                        equipped = true;
                                        break;
                                    }
                                }
                                if (equipped) {
                                    break;
                                }
                            }
                        }
                    } else {
                        // maybe equip diamond_sword
                        for (int i = 0; i < f0.getInventory().getContainerSize(); i++) {
                            ItemStack itemStack = f0.getInventory().getItem(i);
                            if (!itemStack.isEmpty()) {
                                Item item = itemStack.getItem();
                                if (item.toString().contains(target)) {
                                    f0.equipItemIfPossible(itemStack);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (action.startsWith("attack with")) {
                String target = action.replace("attack with", "").trim();
                f0.waveHands(Hand.MAIN_HAND,true);
                f0.getLookControl().setLookAt(toEntity
                        .position()
                        .add(0.0f, 1.0f, 0.0f));
                if (distance <= 6.0f) {
                    boolean didDamage = false;
                    // get sword
                    for (ItemStack itemStack : f0.getHandSlots() ){
                        if (itemStack.isEmpty()) {
                            continue;
                        }
                        Item item = itemStack.getItem();
                        float damage = 0f;
                        if (item instanceof SwordItem) {
                            damage = ((SwordItem) itemStack.getItem()).getDamage();
                            toEntity.hurt(DamageSource.mobAttack(f0), damage);
                        } else if (item instanceof ToolItem) {
                            damage = ((ToolItem) item).getAttackDamage();
                        }
                        if (damage > 0) {
                            didDamage = true;
                            toEntity.hurt(DamageSource.mobAttack(f0), damage);
                            break;
                        }
                    }
                    if (!didDamage) {
                        toEntity.hurt(DamageSource.mobAttack(f0), 0.5f);
                    }
                }
            }


            if (action.startsWith("eat")) {
                String target = action.replace("eat", "").trim();
                if (!f0.getInventory().isEmpty()) {
                    // maybe equip diamond_sword
                    for (int i = 0; i < f0.getInventory().getContainerSize(); i++) {
                        ItemStack itemStack = f0.getInventory().getItem(i);
                        if (!itemStack.isEmpty()) {
                            Item item = itemStack.getItem();
                            if (item.isEdible() && item.toString().contains(target)) {
                                f0.playEatSound(itemStack);
                                break;
                            }
                        }
                    }
                }
            }

            if (action.startsWith("give away") || action.startsWith("give")) {
                String target = action.replace("give away", "").trim().replace("give", "").trim();
                if (!f0.getInventory().isEmpty()) {
                    // maybe equip diamond_sword
                    for (int i = 0; i < f0.getInventory().getContainerSize(); i++) {
                        ItemStack itemStack = f0.getInventory().getItem(i);
                        if (!itemStack.isEmpty()) {
                            Item item = itemStack.getItem();
                            if (item.toString().contains(target)) {
                                ItemStack targetItem = f0.getInventory().removeItem(i, 1);
                                if (!targetItem.isEmpty()) {
                                    f0.waveHands(Hand.MAIN_HAND, false);
                                    toEntity.inventory.add(targetItem);
                                    f0.playTalkSound();
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private static void respondToPotentialActionResponse(NPCVillagerEntity listeningVillager,
                                                         String originalMsg,
                                                         NPCVillagerEntity actionVillager) {
        actionVillager.getLookControl().setLookAt(listeningVillager.position());
        processActionAndTag(originalMsg, actionVillager);
    }

    private static void processActionAndTag(String originalMsg, NPCVillagerEntity actionVillager) {
        Pattern pattern = Pattern.compile("\\(.*\\)");
        boolean hasParentheses = pattern.matcher(originalMsg).find();

        Pattern pattern1 = Pattern.compile("\\((.*?)\\)");

        Matcher macher = pattern1.matcher(originalMsg);
        actionVillager.setCustomTag("");
        if (macher.find()) {
            String action = macher.group(1).toLowerCase();
            if (Config.ActionToEmoji.containsKey(action)) {
                actionVillager.setCustomTag(Config.ActionToEmoji.get(action));
            }
            switch (action) {
                case "jump":
                    actionVillager.getJumpControl().jump();
                    break;
                case "run away":
                    actionVillager.setWalkingControlForceTrigger(true);
                    break;
            }
        }

        if (hasParentheses) {
            if (originalMsg.contains("(jump)")) {
                actionVillager.getJumpControl().jump();
            }

            if (originalMsg.contains("(run away)")) {
                actionVillager.setWalkingControlForceTrigger(true);
            }

//            if (originalMsg.contains("(think)")) {
//                actionVillager.setCustomTag("\uD83E\uDD14");  // 🤔
//            } else {
//                actionVillager.setCustomTag("");
//            }
        }

        actionVillager.playTalkSound();
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
    public void onCommandEvent(CommandEvent event) throws Exception {
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
            if (event.getTarget() instanceof NPCVillagerEntity) {
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
            if (event.getSource().msgId.equals("player")) {
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
    public void onLivingDeathEvent(LivingDeathEvent event) throws Exception {
        if (event.getEntityLiving() instanceof IronGolemEntity) {
            if (event.getSource().msgId.equals("player")) {
                LOGGER.info("Iron Golem has been killed by player");
                PlayerEntity player = (PlayerEntity) event.getSource().getEntity();
                NPCVillagerManager.getInstance().tellAllVillagersTheirGolemHasBeenKilledByPlayer((IronGolemEntity) event.getEntityLiving(), player);
            } else {
                NPCVillagerManager.getInstance().tellAllVillagersTheirGolemHasBeenKilledByPlayer((IronGolemEntity) event.getEntityLiving(), null);
            }
        }
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) throws Exception {
        LOGGER.warn(String.format("[SERVER] Received ServerChatEvent from Player: %s", event.getPlayer().getName().getString()));

        ServerPlayerEntity player = event.getPlayer();
        String userMsg = event.getMessage().toString();
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
        while (iterator.hasNext()) {
            String villagerUUID = Utils.JsonConverter.encodeStringToJson(iterator);
            NPCVillagerEntity obj = NPCVillagerManager.getInstance().getEntityByUUID(villagerUUID);

            obj.setIsTalkingToPlayer(player);
            obj.goalSelector.disableControlFlag(Goal.Flag.MOVE);
            obj.setProcessingMessage(true);

            interactWithEntity(player, originalMsg, obj);
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
        private static final DataParameter<String> CUSTOM_SKIN = EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);
        private static final DataParameter<String> CUSTOM_BACKGROUND_INFO = EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);
        private static final DataParameter<String> CUSTOM_CONTEXT_INFO =
                EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);
        private static final DataParameter<String> CUSTOM_NODE_ID = EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);
        private static final DataParameter<String> CUSTOM_NODE_PUBLIC_ID = EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);
        private static final DataParameter<String> CUSTOM_PROFESSION = EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);
        private static final DataParameter<String> CUSTOM_VILLAGENAME = EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);
        private static final DataParameter<String> CUSTOM_NAME_COLOR = EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);
        private static final DataParameter<String> CUSTOM_TAG =
                EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.STRING);

        private static final DataParameter<Boolean> HAS_AWAKEN =
                EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Boolean> IS_TALKING_WITH_OTHER_VILLAGER =
                EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.BOOLEAN);
        private static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES =
                ImmutableList.of(MemoryModuleType.LIVING_ENTITIES,
                        MemoryModuleType.VISIBLE_LIVING_ENTITIES,
                        MemoryModuleType.NEAREST_PLAYERS, MemoryModuleType.NEAREST_VISIBLE_PLAYER
                        , MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER,
                        MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryModuleType.HURT_BY,
                        MemoryModuleType.HURT_BY_ENTITY, MemoryModuleType.NEAREST_HOSTILE,
                        MemoryModuleType.HEARD_BELL_TIME,
                        MemoryModuleType.GOLEM_DETECTED_RECENTLY,
                        NPCVillagerMod.COMPATRIOTS_MEMORY_TYPE,
                        NPCVillagerMod.PLAYER_ATTACK_HISTORY, NPCVillagerMod.WEATHER_MEMORY,
                        NPCVillagerMod.GOLEM_PROTECTING_MEMORY);
        private static final ImmutableList<SensorType<? extends Sensor<? super NPCVillagerEntity>>> SENSOR_TYPES
                = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS
                , SensorType.NEAREST_ITEMS, SensorType.HURT_BY, SensorType.VILLAGER_HOSTILES,
                SensorType.GOLEM_DETECTED, NPCVillagerMod.COMPATRIOTS_SENSOR_TYPE,
                NPCVillagerMod.WEATHER_SENSOR);
        private static final DataParameter<Boolean> WALKING_CONTROL_FORCE_TRIGGER =
                EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Boolean> WALKING_CONTROL_IS_WAITING_OTHER_VILLAGER =
                EntityDataManager.defineId(NPCVillagerEntity.class, DataSerializers.BOOLEAN);
        public boolean swing_custom_flag = false;
        public int swingTime_custom = -1;
        public float attackAnim_custom = -1;
        private PlayerEntity isTalkingToPlayer = null;
        // When processing message, villager should look at the player, but after sending the
        // message the villager could continue look randomly
        private Boolean isProcessingMessage = false;
        private TextFormatting customNameColor = TextFormatting.WHITE;
        // Refresh intelligence with latest context every 100 ticks by default
        private int remainingRefreshIntelligenceTick = Utils.ENTITYINTELLIGENCE_TICKING_INTERVAL;

        public NPCVillagerEntity(FMLPlayMessages.SpawnEntity packet, World world) {
            this(entity, world);
        }

        public NPCVillagerEntity(EntityType<NPCVillagerEntity> type, World world) {
            super(type, world);

            setBaby(false);
            setNoAi(false);
            setLeftHanded(false);

            this.getNavigation().setCanFloat(true);
            this.setCanPickUpLoot(true);
            this.setCustomSkin(this.getCustomSkin());
            setCustomName(new StringTextComponent("[Awakening...]"));
            setCustomNameVisible(true);
        }

        public Brain<NPCVillagerEntity> getBrain() {
            return (Brain<NPCVillagerEntity>) super.getBrain();
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
            this.entityData.define(CUSTOM_SKIN, Utils.RandomSkinGenerator.generateSkin());
            this.entityData.define(CUSTOM_BACKGROUND_INFO, "");
            this.entityData.define(CUSTOM_PROFESSION, Utils.randomProfession());
            this.entityData.define(CUSTOM_VILLAGENAME, "");
            this.entityData.define(CUSTOM_NAME_COLOR, TextFormatting.BLUE.getName());
            this.entityData.define(CUSTOM_NODE_ID, "");
            this.entityData.define(CUSTOM_NODE_PUBLIC_ID, "");
            this.entityData.define(CUSTOM_CONTEXT_INFO, "");
            this.entityData.define(HAS_AWAKEN, false);
            this.entityData.define(WALKING_CONTROL_FORCE_TRIGGER, false);
            this.entityData.define(WALKING_CONTROL_IS_WAITING_OTHER_VILLAGER, false);
            this.entityData.define(IS_TALKING_WITH_OTHER_VILLAGER,false);
            this.entityData.define(CUSTOM_TAG,"");
        }

        protected void customServerAiStep() {
            this.level.getProfiler().push("villagerBrain");
            this.getBrain().tick((ServerWorld) this.level, this);
            this.level.getProfiler().pop();
            super.customServerAiStep();
        }

        public void aiStep() {
            super.aiStep();
            customUpdateSwingTime();
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

        @OnlyIn(Dist.CLIENT)
        public float getAttackAnim(float p_70678_1_) {
            return this.attackAnim_custom;
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
            if (Config.ENABLE_VILLAGER_RANDOM_CHAT.get()) {
                this.goalSelector.addGoal(3, new NPCVillagerRandomChatGoal(this, 1.0f));
            }
        }

        // FINISHED: Play sound when villager talk

        public void playTalkSound() {
            this.playSound(Utils.RandomAmbientSound.generateSound(), this.getSoundVolume(),
                    this.getVoicePitch());
        }

        public void playEatSound(ItemStack itemStack) {
            this.playSound(this.getEatingSound(itemStack), this.getSoundVolume(),
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

        public Boolean getIsTalkingWithOtherVillager(){
            return this.entityData.get(IS_TALKING_WITH_OTHER_VILLAGER);
        }

        public void setIsTalkingWithOtherVillager(Boolean flag) {
            this.entityData.set(IS_TALKING_WITH_OTHER_VILLAGER, flag);
        }

        public Boolean getControlWalkIsWaitingOtherVillager() {
            return this.entityData.get(WALKING_CONTROL_IS_WAITING_OTHER_VILLAGER);
        }

        public void setWalkingControlIsWaitingOtherVillager(Boolean flag) {
            if (flag) {
                this.goalSelector.disableControlFlag(Goal.Flag.MOVE);
            } else {
                this.goalSelector.enableControlFlag(Goal.Flag.MOVE);
            }
            this.entityData.set(WALKING_CONTROL_IS_WAITING_OTHER_VILLAGER, flag);
        }

        public Boolean getControlWalkForceTrigger() {
            return this.entityData.get(WALKING_CONTROL_FORCE_TRIGGER);
        }

        public void setWalkingControlForceTrigger(Boolean flag) {
            if (flag) {
                this.goalSelector.enableControlFlag(Goal.Flag.MOVE);
            }

            this.entityData.set(WALKING_CONTROL_FORCE_TRIGGER, flag);
        }

        public Boolean getHasAwaken() {
            return this.entityData.get(HAS_AWAKEN);
        }

        public void setHasAwaken(Boolean flag) {
            this.entityData.set(HAS_AWAKEN, flag);
        }

        public String getCustomTag() {
            return this.entityData.get(CUSTOM_TAG);
        }

        public void setCustomTag(String s0) {
            this.entityData.set(CUSTOM_TAG, s0);
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

        public String getCustomProfession() {
            return this.entityData.get(CUSTOM_PROFESSION);
        }

        public void setCustomProfession(String profession) {
            this.entityData.set(CUSTOM_PROFESSION, profession);
        }

        public String getCustomVillagename() {
            return this.entityData.get(CUSTOM_VILLAGENAME);
        }

        public void setCustomVillagename(String villageName) {
            this.entityData.set(CUSTOM_VILLAGENAME, villageName);
        }

        public String getCustomNodeId() {
            return this.entityData.get(CUSTOM_NODE_ID);
        }

        public void setCustomNodeId(String n0) {
            this.entityData.set(CUSTOM_NODE_ID, n0);
        }

        public String getCustomNodePublicId() {
            return this.entityData.get(CUSTOM_NODE_PUBLIC_ID);
        }

        public void setCustomNodePublicId(String n0) {
            this.entityData.set(CUSTOM_NODE_PUBLIC_ID, n0);
        }

        public String getCustomContext() {
            return this.entityData.get(CUSTOM_CONTEXT_INFO);
        }

        public void setCustomContext(String n0) {
            this.entityData.set(CUSTOM_CONTEXT_INFO, n0);
        }
        //endregion

        public void customUpdateSwingTime() {
            int i = 6;
            if (this.swing_custom_flag) {
                ++this.swingTime_custom;
                if (this.swingTime_custom >= i) {
                    this.swingTime_custom = 0;
                    this.swing_custom_flag = false;
                }
            } else {
                this.swingTime_custom = 0;
            }

            this.attackAnim_custom = (float) this.swingTime_custom / (float) i;
        }

        public void waveHands(Hand p_226292_1_, boolean p_226292_2_) {
            if (!this.swing_custom_flag) {
                this.swingTime_custom = -1;
                this.swing_custom_flag = true;
                this.swingingArm = p_226292_1_;
            }
        }

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
            if (t0 != null) {
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
            if (this.getCustomName() != null) {
                p_213281_1_.putString("s_name", this.getCustomName().getString());
            } else {
                p_213281_1_.putString("s_name", "[Awakening...]");
            }
            p_213281_1_.putString("s_villagename", this.getCustomVillagename());
            p_213281_1_.putString("s_profession", this.getCustomProfession());
            p_213281_1_.putString("s_background", this.getCustomBackgroundInfo());
            p_213281_1_.putString("s_skin", this.getCustomSkin());
            p_213281_1_.putString("s_namecolor", this.getCustomNameColor().getName());
            p_213281_1_.putString("s_nodeId", this.getCustomNodeId());
            p_213281_1_.putString("s_nodePublicId", this.getCustomNodePublicId());
            p_213281_1_.putString("s_context", this.getCustomContext());
            p_213281_1_.putBoolean("s_hasAwaken", this.getHasAwaken());
        }

        public void refreshIntelligence() {
            if (!this.getHasAwaken()) {
                return;
            }

            this.asyncRefreshIntelligenceV1(response -> {
                if (response != null) {
                    this.setCustomContext(response.get("prompt"));
                }
            });
        }

        public void asyncRefreshIntelligenceV1(Consumer<Map<String, String>> callback) {
            CompletableFuture.supplyAsync(() -> {
                try {
                    String ssotoken = NPCVillagerManager.getInstance().getSsoToken();
                    if ((ssotoken == null) || ssotoken.equals("")) {
                        LOGGER.error("IMPORTANT! SET YOUR SOCRATES USER AUTHENTICATION IN THE" +
                                " CONFIG FILE " +
                                "UNDER MINECRAFT FOLDER TO INITIATE INTELLIGENTVILLAGER MOD!");
                        return null;
                    }

                    String req = Utils.ContextBuilder.build_prompt_request_body(this.getBrain(), this);

                    Map<String, String> result = NetworkRequestManager.refreshIntelligence(req, ssotoken);

                    if (result.containsKey("msg") && result.get("msg").equals("success")) {
                        return result;
                    }

                    return null;
                } catch (Exception e) {
                    LOGGER.error(e);
                    e.printStackTrace();
                    return null;
                }
            },NetworkRequestManager.executor).thenAccept(callback);
        }

        public void generateIntelligence() {
            this.asyncGenerateIntelligenceV1(response -> {
                if (response != null) {
                    this.setCustomName(new StringTextComponent(response.get("name")));
                    this.setCustomNodeId(response.get("node_id"));
                    this.setCustomNodePublicId(response.get("public_id"));
                    this.setCustomBackgroundInfo(response.get("background"));
                    this.setHasAwaken(true);
                } else {
                    // Generate failed
                    this.remove();
                }
            });
        }

        public void asyncGenerateIntelligenceV1(Consumer<Map<String, String>> callback) {
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
                    String openAIKey = Config.OPENAI_API_KEY.get();
                    Map<String, String> result = NetworkRequestManager.generateIntelligence(customVillagename, customProfession, openAIKey, ssotoken);

                    if (result.containsKey("msg") && result.get("msg").equals("success")) {
                        return result;
                    }

                    return null;
                } catch (Exception e) {
                    LOGGER.error(e);
                    e.printStackTrace();
                    return null;
                }
            },NetworkRequestManager.executor).thenAccept(callback);
        }

        public void interactWithOtherVillager(String originalMsg,
                                          NPCVillagerEntity targetVillager) {
            if (targetVillager != null) {
                targetVillager.setProcessingMessage(true);
                NetworkRequestManager.asyncInteractWithNode(targetVillager.getCustomNodePublicId(),
                        originalMsg,
                        response -> {
                            Map<String, Object> j0 = new HashMap<String, Object>();
                            j0.put("from_entity", targetVillager.getId());
                            j0.put("to_entity", this.getId());
                            if (response != null) {
                                j0.put("code", 200);
                                j0.put("msg", response.trim());
                            } else {
                                j0.put("code", 0);
                                j0.put("msg", Utils.ERROR_MESSAGE);
                            }

                            if (response != null) {
                                respondToPotentialActionResponse(this, response.trim(),
                                        targetVillager);
                            }

                            targetVillager.setProcessingMessage(false);

                            ITextComponent msg = new StringTextComponent(String.format("<villager" +
                                    " response>%s", Utils.JsonConverter.encodeMapToJsonString(j0)));
                            broadcastToOtherPlayerInInteractiveRange(this, msg);

                            if (response != null) {
                                if (targetVillager.position().distanceTo(this.position()) <= Utils.CLOSEST_DISTANCE) {
                                    targetVillager.setIsTalkingWithOtherVillager(true);
                                    this.setIsTalkingWithOtherVillager(true);
                                    targetVillager.interactWithOtherVillager(response.trim(), this);
                                }
                            }
                        });
            }
        }
    }
}
