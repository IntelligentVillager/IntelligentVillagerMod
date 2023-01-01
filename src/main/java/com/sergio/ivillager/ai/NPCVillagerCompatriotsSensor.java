package com.sergio.ivillager.ai;

import com.google.common.collect.ImmutableSet;
import com.sergio.ivillager.NPCVillager;
import com.sergio.ivillager.NPCVillagerManager;
import com.sergio.ivillager.NPCVillagerMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.world.server.ServerWorld;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.sergio.ivillager.NPCVillager.NPCVillagerEntity;

public class NPCVillagerCompatriotsSensor extends Sensor<LivingEntity> {
    protected void doTick(ServerWorld p_212872_1_, LivingEntity p_212872_2_) {
        NPCVillager.LOGGER.info("NPCVillagerCompatriotsSensor ticking!");
        Brain<?> brain = p_212872_2_.getBrain();
        brain.setMemory(NPCVillagerMod.COMPATRIOTS_MEMORY_TYPE,
                NPCVillagerManager.getInstance().findVillagersAtSameVillage((NPCVillagerEntity)p_212872_2_));
    }

    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of();
    }
}
