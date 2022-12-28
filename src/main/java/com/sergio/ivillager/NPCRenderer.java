package com.sergio.ivillager;

import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.client.renderer.entity.MobRenderer;

import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class NPCRenderer {
//    public static class ModelRegisterHandler {
//        private static final String[] IMAGE_NAMES = {
//                "villager_0", "villager_1", "villager_2",
//                "villager_3", "villager_4", "villager_5"
//        };
//
//        @SubscribeEvent
//        @OnlyIn(Dist.CLIENT)
//        public void registerModels(ModelRegistryEvent event) {
//            RenderingRegistry.registerEntityRenderingHandler(NPCVillager.entity,
//                    renderManager -> new MobRenderer(renderManager, new PlayerModel(0, true),
//                            0.5f) {
//
//                        @Override
//                        public ResourceLocation getTextureLocation(Entity p_110775_1_) {
//                            Random rand = new Random();
//                            int index = rand.nextInt(IMAGE_NAMES.length);
//                            return new ResourceLocation("intelligentvillager:textures/entities/" + IMAGE_NAMES[index] +
//                                    ".png");
//                        }
//                    });
//        }
//    }


}
