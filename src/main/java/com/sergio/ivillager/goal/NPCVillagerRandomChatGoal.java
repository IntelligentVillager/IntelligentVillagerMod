package com.sergio.ivillager.goal;

import com.sergio.ivillager.NPCVillager.NPCVillagerEntity;
import com.sergio.ivillager.NPCVillagerManager;
import com.sergio.ivillager.Utils;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumSet;
import java.util.Random;

public class NPCVillagerRandomChatGoal extends Goal {
    protected final NPCVillagerEntity mob;
    public static final Logger LOGGER = LogManager.getLogger(NPCVillagerRandomChatGoal.class);
    protected double wantedX;
    protected double wantedY;
    protected double wantedZ;
    protected final double speedModifier;
    protected NPCVillagerEntity currentTargetEntity;
    private final double closestDistance = Utils.CLOSEST_DISTANCE;


    public NPCVillagerRandomChatGoal(NPCVillagerEntity mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.currentTargetEntity = null;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        try {
            if (!this.mob.getHasAwaken()) {
                return false;
            }

            if (this.mob.getControlWalkForceTrigger() || this.mob.getNavigation().isInProgress()) {
                return false;
            }

            if (this.mob.getIsTalkingToPlayer() != null) {
                return false;
            }

            if (this.mob.getControlWalkIsWaitingOtherVillager()) {
                return false;
            }

            if (this.mob.getIsTalkingWithOtherVillager()) {
                // If a villager is talking with antoehr villagers, it doesn't need to find the next
                // one to talk to\
                if (this.currentTargetEntity == null) {
                    this.mob.setIsTalkingWithOtherVillager(false);
                    return false;
                }
                if (this.currentTargetEntity.position().distanceTo(this.mob.position()) > closestDistance) {
                    this.mob.setIsTalkingWithOtherVillager(false);
                    this.currentTargetEntity.setIsTalkingWithOtherVillager(false);
                    this.currentTargetEntity.setWalkingControlIsWaitingOtherVillager(false);
                    return true;
                } else {
                    return false;
                }
            }

            Random random = new Random();
            if (random.nextInt(10) < 2) {
                NPCVillagerEntity entity_to_talk_to =
                        NPCVillagerManager.getInstance().findTheNextVillagerToTalkTo(this.mob);
                if (entity_to_talk_to != null) {
                    this.currentTargetEntity = entity_to_talk_to;
                    LOGGER.info(String.format("currentTargetEntity successfully set to %s",
                            entity_to_talk_to.getName().getString()));
                    entity_to_talk_to.setWalkingControlIsWaitingOtherVillager(true);
                    Vector3d vector3d = entity_to_talk_to.position();
                    this.wantedX = vector3d.x;
                    this.wantedY = vector3d.y;
                    this.wantedZ = vector3d.z;
                    this.mob.setWalkingControlIsWaitingOtherVillager(false);
                    this.mob.setIsTalkingWithOtherVillager(false);
                    return true;
                }
            }
            this.currentTargetEntity = null;
            return false;
        } catch (Exception e) {
            LOGGER.error(e);
            return false;
        }
    }

    public boolean canContinueToUse() {
        if (!this.mob.getHasAwaken()) {
            return false;
        }

        if (this.mob.getIsTalkingToPlayer() == null && this.currentTargetEntity != null) {
            if (this.currentTargetEntity.position().distanceTo(this.mob.position()) < closestDistance) {
                LOGGER.error(String.format("%s reached position",this.mob.getName().getString()));
                this.mob.setIsTalkingWithOtherVillager(true);
                this.currentTargetEntity.setIsTalkingWithOtherVillager(true);
                this.currentTargetEntity.setWalkingControlIsWaitingOtherVillager(false);
                return false;
            }
            if (this.mob.getNavigation().isStuck()) {
                this.mob.setIsTalkingWithOtherVillager(false);
                this.currentTargetEntity.setIsTalkingWithOtherVillager(false);
                this.currentTargetEntity.setWalkingControlIsWaitingOtherVillager(false);
                this.currentTargetEntity = null;
                return false;
            }
            return true;
        }
        else return false;
    }

    public void start() {
        if (!this.mob.getControlWalkIsWaitingOtherVillager() && !this.mob.getIsTalkingWithOtherVillager()) {
            LOGGER.info(String.format("Villager %s is heading to %s to talk.",
                    this.mob.getName().getString(), this.currentTargetEntity.getName().getString()));
            this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
        }
    }

    public void stop() {
        if (this.currentTargetEntity != null) {
            if (this.currentTargetEntity.position().distanceTo(this.mob.position()) < closestDistance) {
                this.mob.interactWithOtherVillager("hey!", this.currentTargetEntity);
                this.mob.setIsTalkingWithOtherVillager(true);
                this.currentTargetEntity.setIsTalkingWithOtherVillager(true);
                this.currentTargetEntity.setWalkingControlIsWaitingOtherVillager(false);
            } else {
                this.mob.setIsTalkingWithOtherVillager(false);
                this.currentTargetEntity.setIsTalkingWithOtherVillager(false);
                this.currentTargetEntity.setWalkingControlIsWaitingOtherVillager(false);
                this.currentTargetEntity = null;
            }
        }

        super.stop();
    }

    public void tick() {
        if (this.currentTargetEntity != null) {
            if(!this.currentTargetEntity.position().equals(new Vector3d(this.wantedX, this.wantedY
                    , this.wantedZ))){
//                LOGGER.error(String.format("Villager %s was heading to BUT %s is moving",
//                        this.mob.getName().getString(), this.currentTargetEntity.getName().getString()));
                Vector3d vector3d = this.currentTargetEntity.position();
                this.wantedX = vector3d.x;
                this.wantedY = vector3d.y;
                this.wantedZ = vector3d.z;
                this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
            }
        }
    }
}
