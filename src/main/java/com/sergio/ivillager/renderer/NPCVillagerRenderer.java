package com.sergio.ivillager.renderer;

import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HeadLayer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.sergio.ivillager.NPCVillager.CustomEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class NPCVillagerRenderer extends MobRenderer<CustomEntity, PlayerModel<CustomEntity>> {

    public static final Logger LOGGER = LogManager.getLogger(NPCVillagerRenderer.class);

    final CustomLayerRenderer<CustomEntity, PlayerModel<CustomEntity>> customLayerRenderer;

    private static final String[] IMAGE_NAMES = {
            "villager_0", "villager_1", "villager_2",
            "villager_3", "villager_4", "villager_5"
    };


    // TODO: Move back to villager model

    public NPCVillagerRenderer(EntityRendererManager p_i50954_1_) {
        super(p_i50954_1_, new PlayerModel<>(0.0F, true), 0.5F);
        this.addLayer(new HeadLayer<>(this));

        this.customLayerRenderer = new CustomLayerRenderer<>(this);
        this.addLayer(customLayerRenderer);
//        this.addLayer(new CrossedArmsItemLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(CustomEntity p_110775_1_) {
    // TODO: ADD emoji layer

        this.customLayerRenderer.setTexture(new ResourceLocation("intelligentvillager:textures/entities" +
                "/sss.png"));

//        if (NPCVillagerManager.getInstance().isVillagerTalkingtoPlayer(p_110775_1_.getId())) {
//            LOGGER.info(String.format("keeps rendering for entity: %s", p_110775_1_.toString()));
//            this.customLayerRenderer.setTexture(new ResourceLocation("ivillager:textures/entities" +
//                        "/sss.png"));
//            } else {
//            this.customLayerRenderer.setTexture(null);
//        }
//            return new ResourceLocation("ivillager:textures/entities/villager_1_love" +
//                    ".png");

        return new ResourceLocation("intelligentvillager:textures/entities/" + p_110775_1_.getCustomSkin() +
                ".png");
    }
}