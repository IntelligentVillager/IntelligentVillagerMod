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

import java.util.Set;

public class NPCVillagerWeatherSensor extends Sensor<LivingEntity> {
    protected void doTick(ServerWorld p_212872_1_, LivingEntity p_212872_2_) {
        Brain<?> brain = p_212872_2_.getBrain();
        String name = p_212872_2_.getName().getString();

        StringBuilder description = new StringBuilder();
        description.append("It's ");
        if (p_212872_1_.isRaining()) {
            description.append("raining");
            if (p_212872_1_.isThundering()) {
                description.append(" and thundering outside.");
            } else {
                description.append(" outside.");
            }
        } else {
            description.append("cloudy outside.");
        }
        if (p_212872_1_.isRainingAt(p_212872_2_.blockPosition())) {
            description.append(name).append(" is out in the rain.\n");
        } else {
            description.append("While ").append(name).append(" is not in the rain.\n");
        }

        description.append("Time in a day: ");
        if (p_212872_1_.isDay()) {
            description.append("It's still in the middle of the day!\n");
        } else {
            if (p_212872_1_.getSkyDarken() > 8) {
                description.append("It's already night time.\n");
            } else {
                description.append("It's getting darker outside. The night is coming.\n");
            }
        }

        brain.setMemory(NPCVillagerMod.WEATHER_MEMORY,
                description.toString());
    }

    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of();
    }
}
