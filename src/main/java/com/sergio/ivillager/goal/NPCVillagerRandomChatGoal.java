package com.sergio.ivillager.goal;

import com.sergio.ivillager.NPCVillager.NPCVillagerEntity;
import com.sergio.ivillager.NPCVillagerManager;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumSet;

public class NPCVillagerRandomChatGoal extends Goal {
    protected final NPCVillagerEntity mob;
    public static final Logger LOGGER = LogManager.getLogger(NPCVillagerRandomChatGoal.class);
    protected double wantedX;
    protected double wantedY;
    protected double wantedZ;
    protected final double speedModifier;
    protected NPCVillagerEntity currentTargetEntity;


    public NPCVillagerRandomChatGoal(NPCVillagerEntity mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.currentTargetEntity = null;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.mob.getControlWalkForceTrigger() || this.mob.getNavigation().isInProgress()) {
            this.currentTargetEntity = null;
            return false;
        }
        if (this.mob.getIsTalkingToPlayer() != null) {
            this.currentTargetEntity = null;
            return false;
        }

        NPCVillagerEntity entity_to_talk_to =
                NPCVillagerManager.getInstance().findTheNextVillagerToTalkTo(this.mob);
        if (entity_to_talk_to != null) {
            this.currentTargetEntity = entity_to_talk_to;
            entity_to_talk_to.setWalkingControlIsWaitingOtherVillager(true);
            Vector3d vector3d = entity_to_talk_to.position();
            this.wantedX = vector3d.x;
            this.wantedY = vector3d.y;
            this.wantedZ = vector3d.z;
            this.mob.setWalkingControlIsWaitingOtherVillager(false);
            return true;
        }
        this.currentTargetEntity = null;
        return false;
    }

    public boolean canContinueToUse() {
        if (this.mob.getIsTalkingToPlayer() == null && this.currentTargetEntity != null) {
            if (this.currentTargetEntity.position().distanceTo(this.mob.position()) < 5F) {
                return false;
            }
            return true;
        }
        else return false;
    }

    public void start() {
        if (!this.mob.getControlWalkIsWaitingOtherVillager()) {
            this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
        }
    }

    public void stop() {
        this.currentTargetEntity = null;
        super.stop();
    }

    public void tick() {
        if (this.currentTargetEntity != null) {
            if(this.currentTargetEntity.getNavigation().isInProgress()){
                BlockPos vector3d = this.currentTargetEntity.getNavigation().getTargetPos();
                this.wantedX = vector3d.getX();
                this.wantedY = vector3d.getY();
                this.wantedZ = vector3d.getZ();
                this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
            }
        }
    }
}
