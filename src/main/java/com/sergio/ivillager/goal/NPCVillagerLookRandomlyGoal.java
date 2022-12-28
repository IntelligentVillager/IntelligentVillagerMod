package com.sergio.ivillager.goal;

import com.sergio.ivillager.NPCVillager;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

public class NPCVillagerLookRandomlyGoal extends Goal {
    private final NPCVillager.CustomEntity mob;
    private double relX;
    private double relZ;
    private int lookTime;

    public NPCVillagerLookRandomlyGoal(NPCVillager.CustomEntity p_i1647_1_) {
        this.mob = p_i1647_1_;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    public boolean canUse() {

        // TODO: Villager could look randomly when player is typing

        if (this.mob.getIsTalkingToPlayer() != null) {
            return false;
        }
        return this.mob.getRandom().nextFloat() < 0.02F;
    }

    public boolean canContinueToUse() {
        if (this.mob.getIsTalkingToPlayer() != null) {
            return false;
        }
        return this.lookTime >= 0;
    }

    public void start() {
        double d0 = (Math.PI * 2D) * this.mob.getRandom().nextDouble();
        this.relX = Math.cos(d0);
        this.relZ = Math.sin(d0);
        this.lookTime = 20 + this.mob.getRandom().nextInt(20);
    }

    public void tick() {
        --this.lookTime;
        this.mob.getLookControl().setLookAt(this.mob.getX() + this.relX, this.mob.getEyeY(), this.mob.getZ() + this.relZ);
    }
}