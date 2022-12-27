package com.sergio.ivillager;

import com.sergio.ivillager.goal.NPCVillagerLookRandomlyGoal;
import com.sergio.ivillager.goal.NPCVillagerTalkGoal;
import com.sergio.ivillager.goal.NPCVillagerWalkingGoal;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.text.*;
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
        LOGGER.warn("BiomeLoadingEvent");
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
    public void onServerChat(ServerChatEvent event) throws Exception {
        LOGGER.warn("received ServerChatEvent");
        PlayerEntity player = event.getPlayer();

        ArrayList<CustomEntity> nearestVillager =
                NPCVillagerManager.getInstance().getNeareFacedVillager(player);
        if (nearestVillager == null) return;

        for (CustomEntity obj : nearestVillager) {
            NetworkRequestManager.asyncInteractWithNode("9dcf5d19-5c4c-4ae0-a75d-56ad27ea892b",
                    event.getMessage().toString(),
                    response -> {
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

                        player.sendMessage(messageComponent, UUID.randomUUID());
                    });

            obj.setIsTalkingToPlayer(player);
            obj.goalSelector.disableControlFlag(Goal.Flag.MOVE);
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

        private PlayerEntity isTalkingToPlayer = null;
        private String customSkin = "villager_0";

        public CustomEntity(FMLPlayMessages.SpawnEntity packet, World world) {
            this(entity, world);
        }

        public CustomEntity(EntityType<CustomEntity> type, World world) {
            super(type, world);

            setNoAi(false);
            this.getNavigation().setCanFloat(true);
            this.setCanPickUpLoot(true);


            setCustomSkin(Utils.RandomSkinGenerator.generateSkin());
            setCustomName(ITextComponent.nullToEmpty("Jobbs"));
//            setCustomName(new StringTextComponent(Utils.RandomNameGenerator.generateName()));
            setCustomNameVisible(true);
        }

//        @Override
//        public void tick() {
//            super.tick();
//            if (this.isTalkingToPlayer == null) {
//                LOGGER.warn("tick!!!!");
//                this.goalSelector.disableControlFlag(Goal.Flag.MOVE);
//            }
//        }

        @Override
        protected void registerGoals() {
            super.registerGoals();

            this.goalSelector.addGoal(2, new NPCVillagerTalkGoal(this));
            this.goalSelector.addGoal(1, new NPCVillagerLookRandomlyGoal(this));
            this.goalSelector.addGoal(6, new NPCVillagerWalkingGoal(this, 1.0f));
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
    }
}
