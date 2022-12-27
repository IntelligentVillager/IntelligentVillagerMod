package com.sergio.ivillager.goal;

import com.sergio.ivillager.NPCVillagerManager;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;
import com.sergio.ivillager.NPCVillager.CustomEntity;


public class NPCVillagerTalkGoal extends Goal {
    private final CustomEntity mob;

    public NPCVillagerTalkGoal(CustomEntity entity) {
        this.mob = entity;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        entity.getNavigation().setCanFloat(true);
    }

    public boolean canUse() {
        return this.mob.getIsTalkingToPlayer() != null;
    }

    public void tick() {
        if (this.mob.getIsTalkingToPlayer() != null) {
            NPCVillagerManager.LOGGER.warn("TALK Goal activated");
            this.mob.getLookControl().setLookAt(this.mob.getIsTalkingToPlayer().getPosition(0.5f));
        }
    }
}