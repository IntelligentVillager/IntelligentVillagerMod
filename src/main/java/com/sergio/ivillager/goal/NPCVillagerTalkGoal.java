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
    }

    public boolean canUse() {
        return this.mob.getProcessingMessage();
    }

    public void tick() {
        if (this.mob.getProcessingMessage()) {
            // Make the villager look at the player's eyes
            if (this.mob.getIsTalkingToPlayer() != null) {
                this.mob.getLookControl().setLookAt(this.mob
                        .getIsTalkingToPlayer()
//                    .getPosition(0.5f)
                        .position()
                        .add(0.0f, 1.0f, 0.0f));
            }
        }
    }
}