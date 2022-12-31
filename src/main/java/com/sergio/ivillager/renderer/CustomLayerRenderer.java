package com.sergio.ivillager.renderer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.sergio.ivillager.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class CustomLayerRenderer<T extends LivingEntity, M extends PlayerModel<T>> extends LayerRenderer<T,M>{
    private ResourceLocation texture;
    public static final Logger LOGGER = LogManager.getLogger(CustomLayerRenderer.class);

    public CustomLayerRenderer(IEntityRenderer<T, M> entityRendererIn) {
        super(entityRendererIn);
        this.texture = new ResourceLocation(Utils.resourcePathBuilder(
                "textures/entities", "sss.png"));
    }

    @Override
    public void render(MatrixStack p_225628_1_, IRenderTypeBuffer p_225628_2_, int p_225628_3_, T p_225628_4_, float p_225628_5_, float p_225628_6_, float p_225628_7_, float p_225628_8_, float p_225628_9_, float p_225628_10_) {

        if (this.texture == null){
            return;
        }
//        LOGGER.warn("finally rendering!!");

        Minecraft mc = Minecraft.getInstance();
        TextureManager textureManager = mc.getTextureManager();
        IRenderTypeBuffer buffer =mc.renderBuffers().bufferSource();

        IVertexBuilder vertexBuilder = buffer.getBuffer(RenderType.entityCutout(this.texture));

        textureManager.bind(this.texture);

        p_225628_1_.pushPose();

        p_225628_1_.translate(0.0, -1.4, 0.0);
        p_225628_1_.mulPose(new Quaternion(0.0F, 1.0F, 0.0F, (float) Math.toRadians(0.0F)));
        p_225628_1_.scale(0.5f, 0.5f, 0.5f);

        vertexBuilder.vertex(p_225628_1_.last().pose(), -1.0F, -1.0F, 0.0F)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(p_225628_1_.last().normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
        vertexBuilder.vertex(p_225628_1_.last().pose(), 1.0F, -1.0F, 0.0F)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(p_225628_1_.last().normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
        vertexBuilder.vertex(p_225628_1_.last().pose(), 1.0F, 1.0F, 0.0F)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(p_225628_1_.last().normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
        vertexBuilder.vertex(p_225628_1_.last().pose(), -1.0F, 1.0F, 0.0F)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(p_225628_1_.last().normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();

// Construct the geometry of the custom layer using the MatrixStack and IVertexBuilder

        p_225628_1_.popPose();


        // display image from behind the villager

        p_225628_1_.pushPose();

        p_225628_1_.translate(0.0, -1.4, 0.0);
//        p_225628_1_.mulPose(new Quaternion(0.0F, 1.0F, 0.0F, (float) Math.toRadians(0.0F)));
        p_225628_1_.mulPose(new Quaternion(0.0F, 0.0F, 0.0F, true));
        p_225628_1_.scale(0.5f, 0.5f, 0.5f);

        vertexBuilder.vertex(p_225628_1_.last().pose(), -1.0F, -1.0F, 0.0F)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(p_225628_1_.last().normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
        vertexBuilder.vertex(p_225628_1_.last().pose(), 1.0F, -1.0F, 0.0F)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(p_225628_1_.last().normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
        vertexBuilder.vertex(p_225628_1_.last().pose(), 1.0F, 1.0F, 0.0F)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(p_225628_1_.last().normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
        vertexBuilder.vertex(p_225628_1_.last().pose(), -1.0F, 1.0F, 0.0F)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240)
                .normal(p_225628_1_.last().normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();

        // Construct the geometry of the custom layer using the MatrixStack and IVertexBuilder
        p_225628_1_.popPose();
    }

    public void setTexture(ResourceLocation texture) {
        this.texture = texture;
    }


//    @Override
//    public void render(MatrixStack p_225628_1_, IRenderTypeBuffer p_225628_2_, int p_225628_3_, Entity p_225628_4_, float p_225628_5_, float p_225628_6_, float p_225628_7_, float p_225628_8_, float p_225628_9_, float p_225628_10_) {
//
//    }
}
