package com.sergio.ivillager;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ParsedArgument;
import com.sergio.ivillager.goal.NPCVillagerLookRandomlyGoal;
import com.sergio.ivillager.goal.NPCVillagerTalkGoal;
import com.sergio.ivillager.goal.NPCVillagerWalkingGoal;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SoundEvents;
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

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@NPCModElement.ModElement.Tag
@Mod.EventBusSubscriber
public class NPCVillager extends NPCModElement.ModElement {

    public static final Logger LOGGER = LogManager.getLogger(NPCVillager.class);

    public static EntityType entity = (EntityType.Builder.<CustomEntity>of(CustomEntity::new,
                    EntityClassification.MISC)
            .setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(CustomEntity::new)
            .sized(0.6f, 1.95f)).build("test_ainpc").setRegistryName("test_ainpc");

    public NPCVillager(NPCModElement instance) {
        super(instance, 1);
//        FMLJavaModLoadingContext.get().getModEventBus().register(new NPCRenderer.ModelRegisterHandler());
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
        if (entity instanceof CustomEntity) {
            NPCVillagerManager.getInstance().addVillager((CustomEntity) entity);
        }
        if (entity instanceof PlayerEntity) {
            NPCVillagerManager.getInstance().updateVillagers((PlayerEntity) entity);
        }
    }

    @SubscribeEvent
    public static void mobEvent(LivingSpawnEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof CustomEntity) {
            NPCVillagerManager.getInstance().addVillager((CustomEntity) entity);
        }
        if (entity instanceof VillagerEntity) {
            if (!(entity instanceof CustomEntity)) {
                CustomEntity customVillager = (CustomEntity) NPCVillager.entity.create((World) event.getWorld());
                // set the custom villager's position to the village center
                customVillager.setPos(entity.position().x + 1, entity.position().y + 1,
                        entity.position().z);
                // add the custom villager to the world
                event.getWorld().addFreshEntity(customVillager);

                // add the custom villager to singelton manager
                NPCVillagerManager.getInstance().addVillager(customVillager);

                // remove the original villager
                entity.remove();
            }
        }
    }

    @SubscribeEvent
    public void onCommandEvent(CommandEvent event) throws  Exception {
        LOGGER.warn("received command event");
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
        LOGGER.warn("received ServerChatEvent");
        PlayerEntity player = event.getPlayer();

        ArrayList<CustomEntity> nearestVillager =
                NPCVillagerManager.getInstance().getNeareFacedVillager(player);
        if (nearestVillager == null) return;
        String userMsg  = event.getMessage().toString();
        if (userMsg.startsWith("<villager command>")) {
            event.setCanceled(true);
            player.sendMessage(new StringTextComponent(String.format("<%s> %s", player.getName().getString(), userMsg)), player.getUUID());
            userMsg = userMsg.replace("<villager command> ", "");
        } else {
            return;
        }

        for (CustomEntity obj : nearestVillager) {

            obj.setIsTalkingToPlayer(player);
            obj.goalSelector.disableControlFlag(Goal.Flag.MOVE);
            obj.setProcessingMessage(true);

            NetworkRequestManager.asyncInteractWithNode(player.getUUID(), Utils.TEST_NODE_ID,
                    userMsg,
                    response -> {
                        if (response != null) {
                            ITextComponent nameString = new StringTextComponent(String.format("<%s>",
                                    obj.getCustomName().getString()))
                                    .setStyle(Style.EMPTY.withColor(TextFormatting.BLUE));
                            ITextComponent contentString = new StringTextComponent(response)
                                    .setStyle(Style.EMPTY);

                            TextComponent messageComponent = new TextComponent() {
                                @Override
                                public TextComponent plainCopy() {
                                    return null;
                                }
                            };

                            messageComponent.append(nameString);
                            messageComponent.append(contentString);

                            ITextComponent msg = new StringTextComponent(String.format("<villager response><%s> %s", obj.getCustomName().getString(), response));

                            obj.getLookControl().setLookAt(obj.getIsTalkingToPlayer().position());

                            player.sendMessage(msg, player.getUUID());

                            if (response.startsWith("*") && response.endsWith("*")) {
                                obj.eatAndDigestFood();
                                obj.setJumping(true);
                            }

                            obj.playTalkSound();
                        } else {
                            // Send default error message to the client indicating some error occurs
                            ITextComponent errorString = new StringTextComponent(Utils.ERROR_MESSAGE)
                                    .setStyle(Style.EMPTY.withColor(TextFormatting.DARK_RED));
                            player.sendMessage(errorString, UUID.randomUUID());
                        }
                        obj.setProcessingMessage(false);
                    });
            // TODO: 多个 NPC 一起，只取第一个？
            break;
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

    public static class CustomEntity extends NPCVillagerBaseEntity {

        // TODO: Rename the entity to NPCVillagerEntity

        private PlayerEntity isTalkingToPlayer = null;

        // When processing message, villager should look at the player, but after sending the
        // message the villager could continue look randomly
        private Boolean isProcessingMessage = false;

        private String customSkin = "villager_0";


        public CustomEntity(FMLPlayMessages.SpawnEntity packet, World world) {
            this(entity, world);
        }

        public CustomEntity(EntityType<CustomEntity> type, World world) {
            super(type, world);

            setBaby(false);
            setNoAi(false);

            this.getNavigation().setCanFloat(true);
            this.setCanPickUpLoot(true);

            setCustomSkin(Utils.RandomSkinGenerator.generateSkin());

            // TODO: Name and personal background should be generated from GPT3 API instead of
            //  setting it manually
//            setCustomName(ITextComponent.nullToEmpty("Jobbs"));
            setCustomName(new StringTextComponent("Jobbs"));
//            setCustomName(new StringTextComponent(Utils.RandomNameGenerator.generateName()));
            setCustomNameVisible(true);
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
            return customSkin;
        }

        public void setCustomSkin(String customSkin) {
            this.customSkin = customSkin;
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
    }
}
