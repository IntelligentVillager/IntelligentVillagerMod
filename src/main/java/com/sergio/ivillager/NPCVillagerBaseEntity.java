package com.sergio.ivillager;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.merchant.IReputationTracking;
import net.minecraft.entity.merchant.IReputationType;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.merchant.villager.VillagerData;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.entity.monster.WitchEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.villager.IVillagerDataHolder;
import net.minecraft.entity.villager.VillagerType;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.network.DebugPacketSender;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.village.GossipManager;
import net.minecraft.village.GossipType;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.raid.Raid;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.FMLPlayMessages;

import javax.annotation.Nullable;
import java.util.Map;

public class NPCVillagerBaseEntity extends AbstractVillagerEntity implements IReputationTracking, IVillagerDataHolder {

    public static EntityType entity = (EntityType.Builder.<NPCVillagerBaseEntity>of(NPCVillagerBaseEntity::new,
                    EntityClassification.MISC)
            .setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).setCustomClientFactory(NPCVillagerBaseEntity::new)
            .sized(0.6f, 1.00f)).build("test_ainpc_base").setRegistryName("test_ainpc_base");

    private static final DataParameter<VillagerData> DATA_VILLAGER_DATA = EntityDataManager.defineId(NPCVillagerBaseEntity.class, DataSerializers.VILLAGER_DATA);
    public static final Map<Item, Integer> FOOD_POINTS = ImmutableMap.of(Items.BREAD, 4, Items.POTATO, 1, Items.CARROT, 1, Items.BEETROOT, 1);
    private byte foodLevel;
    private final GossipManager gossips = new GossipManager();
    private long lastGossipTime;
    private long lastGossipDecayTime;
    private int villagerXp;
    public NPCVillagerBaseEntity(EntityType<? extends NPCVillagerBaseEntity> p_i50183_1_, World p_i50183_2_) {
        super(p_i50183_1_, p_i50183_2_);
        ((GroundPathNavigator)this.getNavigation()).setCanOpenDoors(true);
        this.getNavigation().setCanFloat(true);
        this.setCanPickUpLoot(true);
        this.setVillagerData(this.getVillagerData().setType(VillagerType.PLAINS).setProfession(VillagerProfession.NONE));
    }

    public NPCVillagerBaseEntity(FMLPlayMessages.SpawnEntity spawnEntity, World world) {
        this(entity, world);
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return MobEntity.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5D).add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    protected void customServerAiStep() {
        this.level.getProfiler().push("villagerBrain");
        this.level.getProfiler().pop();

        if (!this.isNoAi() && this.random.nextInt(100) == 0) {
            Raid raid = ((ServerWorld)this.level).getRaidAt(this.blockPosition());
            if (raid != null && raid.isActive() && !raid.isOver()) {
                this.level.broadcastEntityEvent(this, (byte)42);
            }
        }

        if (this.getVillagerData().getProfession() == VillagerProfession.NONE && this.isTrading()) {
            this.stopTrading();
        }

        super.customServerAiStep();
    }

    public void tick() {
        super.tick();
        if (this.getUnhappyCounter() > 0) {
            this.setUnhappyCounter(this.getUnhappyCounter() - 1);
        }

        this.maybeDecayGossip();
    }

    public ActionResultType mobInteract(PlayerEntity p_230254_1_, Hand p_230254_2_) {
        ItemStack itemstack = p_230254_1_.getItemInHand(p_230254_2_);
        if (itemstack.getItem() != Items.VILLAGER_SPAWN_EGG && this.isAlive() && !this.isTrading() && !this.isSleeping() && !p_230254_1_.isSecondaryUseActive()) {
            if (this.isBaby()) {
                this.setUnhappy();
                return ActionResultType.sidedSuccess(this.level.isClientSide);
            } else {
                boolean flag = this.getOffers().isEmpty();
                if (p_230254_2_ == Hand.MAIN_HAND) {
                    if (flag && !this.level.isClientSide) {
                        this.setUnhappy();
                    }

                    p_230254_1_.awardStat(Stats.TALKED_TO_VILLAGER);
                }

                if (flag) {
                    return ActionResultType.sidedSuccess(this.level.isClientSide);
                } else {
                    return ActionResultType.sidedSuccess(this.level.isClientSide);
                }
            }
        } else {
            return super.mobInteract(p_230254_1_, p_230254_2_);
        }
    }

    private void setUnhappy() {
        this.setUnhappyCounter(40);
        if (!this.level.isClientSide()) {
            this.playSound(SoundEvents.VILLAGER_NO, this.getSoundVolume(), this.getVoicePitch());
        }
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VILLAGER_DATA, new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
    }

    public void addAdditionalSaveData(CompoundNBT p_213281_1_) {
        super.addAdditionalSaveData(p_213281_1_);
        VillagerData.CODEC.encodeStart(NBTDynamicOps.INSTANCE, this.getVillagerData()).resultOrPartial(LOGGER::error).ifPresent((p_234547_1_) -> {
            p_213281_1_.put("VillagerData", p_234547_1_);
        });
        p_213281_1_.putByte("FoodLevel", this.foodLevel);
        p_213281_1_.put("Gossips", this.gossips.store(NBTDynamicOps.INSTANCE).getValue());
        p_213281_1_.putInt("Xp", this.villagerXp);
        p_213281_1_.putLong("LastRestock", 0L);
        p_213281_1_.putLong("LastGossipDecay", this.lastGossipDecayTime);
        p_213281_1_.putInt("RestocksToday", 0);
        p_213281_1_.putBoolean ("AssignProfessionWhenSpawned", true);
    }

    public void readAdditionalSaveData(CompoundNBT p_70037_1_) {
        super.readAdditionalSaveData(p_70037_1_);
        if (p_70037_1_.contains("VillagerData", 10)) {
            DataResult<VillagerData> dataresult = VillagerData.CODEC.parse(new Dynamic<>(NBTDynamicOps.INSTANCE, p_70037_1_.get("VillagerData")));
            dataresult.resultOrPartial(LOGGER::error).ifPresent(this::setVillagerData);
        }

        if (p_70037_1_.contains("Offers", 10)) {
            this.offers = new MerchantOffers(p_70037_1_.getCompound("Offers"));
        }

        if (p_70037_1_.contains("FoodLevel", 1)) {
            this.foodLevel = p_70037_1_.getByte("FoodLevel");
        }

        ListNBT listnbt = p_70037_1_.getList("Gossips", 10);
        this.gossips.update(new Dynamic<>(NBTDynamicOps.INSTANCE, listnbt));
        if (p_70037_1_.contains("Xp", 3)) {
            this.villagerXp = p_70037_1_.getInt("Xp");
        }

        this.lastGossipDecayTime = p_70037_1_.getLong("LastGossipDecay");
        this.setCanPickUpLoot(true);
    }

    public boolean removeWhenFarAway(double p_213397_1_) {
        return false;
    }

    @Nullable
    protected SoundEvent getAmbientSound() {
        return null;
    }

    protected SoundEvent getHurtSound(DamageSource p_184601_1_) {
        return SoundEvents.VILLAGER_HURT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    public void playWorkSound() {
        SoundEvent soundevent = this.getVillagerData().getProfession().getWorkSound();
        if (soundevent != null) {
            this.playSound(soundevent, this.getSoundVolume(), this.getVoicePitch());
        }

    }

    public void setVillagerData(VillagerData p_213753_1_) {
        VillagerData villagerdata = this.getVillagerData();
        if (villagerdata.getProfession() != p_213753_1_.getProfession()) {
            this.offers = null;
        }

        this.entityData.set(DATA_VILLAGER_DATA, p_213753_1_);
    }

    public VillagerData getVillagerData() {
        return this.entityData.get(DATA_VILLAGER_DATA);
    }

    public void setLastHurtByMob(@Nullable LivingEntity p_70604_1_) {
        if (p_70604_1_ != null && this.level instanceof ServerWorld) {
            ((ServerWorld)this.level).onReputationEvent(IReputationType.VILLAGER_HURT, p_70604_1_, this);
            if (this.isAlive() && p_70604_1_ instanceof PlayerEntity) {
                this.level.broadcastEntityEvent(this, (byte)13);
            }
        }

        super.setLastHurtByMob(p_70604_1_);
    }

    public void die(DamageSource p_70645_1_) {
        LOGGER.info("Villager {} died, message: '{}'", this, p_70645_1_.getLocalizedDeathMessage(this).getString());
        Entity entity = p_70645_1_.getEntity();
        super.die(p_70645_1_);
    }

    @Override
    protected void updateTrades() {

    }

    public boolean canBreed() {
        return false;
    }

    private boolean hungry() {
        return this.foodLevel < 12;
    }

    private void eatUntilFull() {
        if (this.hungry() && this.countFoodPointsInInventory() != 0) {
            for(int i = 0; i < this.getInventory().getContainerSize(); ++i) {
                ItemStack itemstack = this.getInventory().getItem(i);
                if (!itemstack.isEmpty()) {
                    Integer integer = FOOD_POINTS.get(itemstack.getItem());
                    if (integer != null) {
                        int j = itemstack.getCount();

                        for(int k = j; k > 0; --k) {
                            this.foodLevel = (byte)(this.foodLevel + integer);
                            this.getInventory().removeItem(i, 1);
                            if (!this.hungry()) {
                                return;
                            }
                        }
                    }
                }
            }

        }
    }

    public int getPlayerReputation(PlayerEntity p_223107_1_) {
        return this.gossips.getReputation(p_223107_1_.getUUID(), (p_223103_0_) -> {
            return true;
        });
    }

    private void digestFood(int p_213758_1_) {
        this.foodLevel = (byte)(this.foodLevel - p_213758_1_);
    }

    public void eatAndDigestFood() {
        this.eatUntilFull();
        this.digestFood(12);
    }

    private boolean shouldIncreaseLevel() {
        int i = this.getVillagerData().getLevel();
        return VillagerData.canLevelUp(i) && this.villagerXp >= VillagerData.getMaxXpPerLevel(i);
    }

    protected ITextComponent getTypeName() {
        net.minecraft.util.ResourceLocation profName = this.getVillagerData().getProfession().getRegistryName();
        return new TranslationTextComponent(this.getType().getDescriptionId() + '.' + (!"minecraft".equals(profName.getNamespace()) ? profName.getNamespace() + '.' : "") + profName.getPath());
    }

    @OnlyIn(Dist.CLIENT)
    public void handleEntityEvent(byte p_70103_1_) {
        if (p_70103_1_ == 12) {
            this.addParticlesAroundSelf(ParticleTypes.HEART);
        } else if (p_70103_1_ == 13) {
            this.addParticlesAroundSelf(ParticleTypes.ANGRY_VILLAGER);
        } else if (p_70103_1_ == 14) {
            this.addParticlesAroundSelf(ParticleTypes.HAPPY_VILLAGER);
        } else if (p_70103_1_ == 42) {
            this.addParticlesAroundSelf(ParticleTypes.SPLASH);
        } else {
            super.handleEntityEvent(p_70103_1_);
        }
    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld p_213386_1_, DifficultyInstance p_213386_2_, SpawnReason p_213386_3_, @Nullable ILivingEntityData p_213386_4_, @Nullable CompoundNBT p_213386_5_) {
        if (p_213386_3_ == SpawnReason.BREEDING) {
            this.setVillagerData(this.getVillagerData().setProfession(VillagerProfession.NONE));
        }

        if (p_213386_3_ == SpawnReason.COMMAND || p_213386_3_ == SpawnReason.SPAWN_EGG || p_213386_3_ == SpawnReason.SPAWNER || p_213386_3_ == SpawnReason.DISPENSER) {
            this.setVillagerData(this.getVillagerData().setType(VillagerType.byBiome(p_213386_1_.getBiomeName(this.blockPosition()))));
        }

        return super.finalizeSpawn(p_213386_1_, p_213386_2_, p_213386_3_, p_213386_4_, p_213386_5_);
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return null;
    }

    public void thunderHit(ServerWorld p_241841_1_, LightningBoltEntity p_241841_2_) {
        if (p_241841_1_.getDifficulty() != Difficulty.PEACEFUL && net.minecraftforge.event.ForgeEventFactory.canLivingConvert(this, EntityType.WITCH, (timer) -> {})) {
            LOGGER.info("Villager {} was struck by lightning {}.", this, p_241841_2_);
            WitchEntity witchentity = EntityType.WITCH.create(p_241841_1_);
            witchentity.moveTo(this.getX(), this.getY(), this.getZ(), this.yRot, this.xRot);
            witchentity.finalizeSpawn(p_241841_1_, p_241841_1_.getCurrentDifficultyAt(witchentity.blockPosition()), SpawnReason.CONVERSION, (ILivingEntityData)null, (CompoundNBT)null);
            witchentity.setNoAi(this.isNoAi());
            if (this.hasCustomName()) {
                witchentity.setCustomName(this.getCustomName());
                witchentity.setCustomNameVisible(this.isCustomNameVisible());
            }

            witchentity.setPersistenceRequired();
            net.minecraftforge.event.ForgeEventFactory.onLivingConvert(this, witchentity);
            p_241841_1_.addFreshEntityWithPassengers(witchentity);
            this.remove();
        } else {
            super.thunderHit(p_241841_1_, p_241841_2_);
        }

    }

    protected void pickUpItem(ItemEntity p_175445_1_) {
        ItemStack itemstack = p_175445_1_.getItem();
        if (this.wantsToPickUp(itemstack)) {
            Inventory inventory = this.getInventory();
            boolean flag = inventory.canAddItem(itemstack);
            if (!flag) {
                return;
            }

            this.onItemPickup(p_175445_1_);
            this.take(p_175445_1_, itemstack.getCount());
            ItemStack itemstack1 = inventory.addItem(itemstack);
            if (itemstack1.isEmpty()) {
                p_175445_1_.remove();
            } else {
                itemstack.setCount(itemstack1.getCount());
            }
        }

    }

    public boolean wantsToPickUp(ItemStack p_230293_1_) {
        Item item = p_230293_1_.getItem();
        return this.getInventory().canAddItem(p_230293_1_);
    }

    public boolean hasExcessFood() {
        return this.countFoodPointsInInventory() >= 24;
    }

    public boolean wantsMoreFood() {
        return this.countFoodPointsInInventory() < 12;
    }

    private int countFoodPointsInInventory() {
        Inventory inventory = this.getInventory();
        return FOOD_POINTS.entrySet().stream().mapToInt((p_226553_1_) -> {
            return inventory.countItem(p_226553_1_.getKey()) * p_226553_1_.getValue();
        }).sum();
    }

    public boolean hasFarmSeeds() {
        return this.getInventory().hasAnyOf(ImmutableSet.of(Items.WHEAT_SEEDS, Items.POTATO, Items.CARROT, Items.BEETROOT_SEEDS));
    }

    public void gossip(ServerWorld p_242368_1_, NPCVillagerBaseEntity p_242368_2_, long p_242368_3_) {
        if ((p_242368_3_ < this.lastGossipTime || p_242368_3_ >= this.lastGossipTime + 1200L) && (p_242368_3_ < p_242368_2_.lastGossipTime || p_242368_3_ >= p_242368_2_.lastGossipTime + 1200L)) {
            this.gossips.transferFrom(p_242368_2_.gossips, this.random, 10);
            this.lastGossipTime = p_242368_3_;
            p_242368_2_.lastGossipTime = p_242368_3_;
        }
    }

    private void maybeDecayGossip() {
        long i = this.level.getGameTime();
        if (this.lastGossipDecayTime == 0L) {
            this.lastGossipDecayTime = i;
        } else if (i >= this.lastGossipDecayTime + 24000L) {
            this.gossips.decay();
            this.lastGossipDecayTime = i;
        }
    }

    public void onReputationEventFrom(IReputationType p_213739_1_, Entity p_213739_2_) {
        if (p_213739_1_ == IReputationType.ZOMBIE_VILLAGER_CURED) {
            this.gossips.add(p_213739_2_.getUUID(), GossipType.MAJOR_POSITIVE, 20);
            this.gossips.add(p_213739_2_.getUUID(), GossipType.MINOR_POSITIVE, 25);
        } else if (p_213739_1_ == IReputationType.TRADE) {
            this.gossips.add(p_213739_2_.getUUID(), GossipType.TRADING, 2);
        } else if (p_213739_1_ == IReputationType.VILLAGER_HURT) {
            this.gossips.add(p_213739_2_.getUUID(), GossipType.MINOR_NEGATIVE, 25);
        } else if (p_213739_1_ == IReputationType.VILLAGER_KILLED) {
            this.gossips.add(p_213739_2_.getUUID(), GossipType.MAJOR_NEGATIVE, 25);
        }

    }

    public int getVillagerXp() {
        return this.villagerXp;
    }

    @Override
    protected void rewardTradeXp(MerchantOffer p_213713_1_) {
        return;
    }

    public void setVillagerXp(int p_213761_1_) {
        this.villagerXp = p_213761_1_;
    }

    public GossipManager getGossips() {
        return this.gossips;
    }

    public void setGossips(INBT p_223716_1_) {
        this.gossips.update(new Dynamic<>(NBTDynamicOps.INSTANCE, p_223716_1_));
    }

    protected void sendDebugPackets() {
        super.sendDebugPackets();
        DebugPacketSender.sendEntityBrain(this);
    }

    public void startSleeping(BlockPos p_213342_1_) {
        super.startSleeping(p_213342_1_);
        this.brain.setMemory(MemoryModuleType.LAST_SLEPT, this.level.getGameTime());
        this.brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        this.brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }

    public void stopSleeping() {
        super.stopSleeping();
        this.brain.setMemory(MemoryModuleType.LAST_WOKEN, this.level.getGameTime());
    }
}
