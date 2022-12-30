package com.sergio.ivillager.goal;

import com.sergio.ivillager.NPCVillager;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.vector.Vector3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumSet;

public class NPCVillagerLookRandomlyGoal extends Goal {

    public static final Logger LOGGER = LogManager.getLogger(NPCVillagerLookRandomlyGoal.class);

    private final NPCVillager.NPCVillagerEntity mob;
    private double relX;
    private double relZ;
    private Vector3d lookTarget = Vector3d.ZERO;

    private Vector3d lastSeeingPlayerPosition = Vector3d.ZERO;

    private int lookTime;

    public NPCVillagerLookRandomlyGoal(NPCVillager.NPCVillagerEntity p_i1647_1_) {
        this.mob = p_i1647_1_;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    public boolean canUse() {

        // FINISHED: Villager could look randomly when player is typing

        if (this.mob.getProcessingMessage()) {
            return false;
        }
        return this.mob.getRandom().nextFloat() < 0.02F;
    }

    public boolean canContinueToUse() {
        if (this.mob.getProcessingMessage()) {
            return false;
        }
        return this.lookTime >= 0;
    }

    public void start() {
        double d0 = (Math.PI * 2D) * this.mob.getRandom().nextDouble();
        double d1 = Math.random();

//        if (this.mob.getIsTalkingToPlayer() != null && !this.lastSeeingPlayerPosition.equals(this.mob.getIsTalkingToPlayer().getPosition(0.5f)))
        if (this.mob.getIsTalkingToPlayer() != null && !this.lastSeeingPlayerPosition.equals(this.mob.getIsTalkingToPlayer().position()))
        {
            d1 = 0.0;
//            this.lastSeeingPlayerPosition = this.mob.getIsTalkingToPlayer().getPosition(0.5f);
            this.lastSeeingPlayerPosition = this.mob.getIsTalkingToPlayer().position();
        }

        this.relX = Math.cos(d0);
        this.relZ = Math.sin(d0);

        if (this.mob.getIsTalkingToPlayer() != null) {

            if (d1 < 0.5) {
                this.lookTarget = this.mob
                        .getIsTalkingToPlayer()
//                        .getPosition(0.5f)
                        .position()
                        .add(0.0f, 1.0f, 0.0f);
            } else {
                this.lookTarget = new Vector3d(this.mob.getX() + this.relX, this.mob.getEyeY(),
                        this.mob.getZ() + this.relZ);
            }
        } else {
            this.lookTarget = new Vector3d(this.mob.getX() + this.relX, this.mob.getEyeY(),
                    this.mob.getZ() + this.relZ);
        }

        this.lookTime = 5 + this.mob.getRandom().nextInt(20);
    }

    public void tick() {
        --this.lookTime;
        this.mob.getLookControl().setLookAt(this.lookTarget);
        if (this.mob.getIsTalkingToPlayer() == null) {
            // remove last seeing player information
            this.lastSeeingPlayerPosition = Vector3d.ZERO;
        }
    }
}