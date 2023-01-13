package com.sergio.ivillager.renderer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.sergio.ivillager.EmojiManager;
import com.sergio.ivillager.Utils;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.Entity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.UseAction;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.sergio.ivillager.NPCVillager.NPCVillagerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class NPCVillagerRenderer extends LivingRenderer<NPCVillagerEntity, BipedModel<NPCVillagerEntity>> {

    public static final Logger LOGGER = LogManager.getLogger(NPCVillagerRenderer.class);

    final CustomLayerRenderer<NPCVillagerEntity, BipedModel<NPCVillagerEntity>> customLayerRenderer;

    public NPCVillagerRenderer(EntityRendererManager p_i46102_1_) {
        this(p_i46102_1_, false);
    }

    public NPCVillagerRenderer(EntityRendererManager p_i46103_1_, boolean p_i46103_2_) {
        super(p_i46103_1_, new PlayerModel<>(0.0F, p_i46103_2_), 0.5F);
        this.addLayer(new BipedArmorLayer<>(this, new BipedModel(0.5F), new BipedModel(1.0F)));
        this.addLayer(new HeldItemLayer<>(this));
        this.addLayer(new HeadLayer<>(this));
        this.addLayer(new ElytraLayer<>(this));

        this.customLayerRenderer = new CustomLayerRenderer<>(this);
        this.addLayer(customLayerRenderer);
    }

    private static BipedModel.ArmPose getArmPose(NPCVillagerEntity p_241741_0_, Hand p_241741_1_) {
        ItemStack itemstack = p_241741_0_.getItemInHand(p_241741_1_);
        if (itemstack.isEmpty()) {
            return BipedModel.ArmPose.EMPTY;
        } else {
            if (p_241741_0_.getUsedItemHand() == p_241741_1_ && p_241741_0_.getUseItemRemainingTicks() > 0) {
                UseAction useaction = itemstack.getUseAnimation();
                if (useaction == UseAction.BLOCK) {
                    return BipedModel.ArmPose.BLOCK;
                }

                if (useaction == UseAction.BOW) {
                    return BipedModel.ArmPose.BOW_AND_ARROW;
                }

                if (useaction == UseAction.SPEAR) {
                    return BipedModel.ArmPose.THROW_SPEAR;
                }

                if (useaction == UseAction.CROSSBOW && p_241741_1_ == p_241741_0_.getUsedItemHand()) {
                    return BipedModel.ArmPose.CROSSBOW_CHARGE;
                }
            } else if (!p_241741_0_.swinging && itemstack.getItem() == Items.CROSSBOW && CrossbowItem.isCharged(itemstack)) {
                return BipedModel.ArmPose.CROSSBOW_HOLD;
            }

            return BipedModel.ArmPose.ITEM;
        }
    }

    public void render(NPCVillagerEntity p_225623_1_, float p_225623_2_, float p_225623_3_, MatrixStack p_225623_4_, IRenderTypeBuffer p_225623_5_, int p_225623_6_) {
        this.setModelProperties(p_225623_1_);
        if (p_225623_1_.getCustomTag().equals("")) {
            customLayerRenderer.isVisible = false;
        } else {
            customLayerRenderer.isVisible = true;
            ResourceLocation texture = EmojiManager.getInstance().getTextureByEmoji(p_225623_1_.getCustomTag());
            if (texture != null) {
                customLayerRenderer.setTexture(texture);
            }
        }

        if (p_225623_1_.swing_custom_flag) {
            this.model.attackTime = p_225623_1_.attackAnim_custom;
        } else {
            this.model.attackTime = 0.0f;
        }

        super.render(p_225623_1_, p_225623_2_, p_225623_3_, p_225623_4_, p_225623_5_, p_225623_6_);
    }

    public Vector3d getRenderOffset(NPCVillagerEntity p_225627_1_, float p_225627_2_) {
        return p_225627_1_.isCrouching() ? new Vector3d(0.0D, -0.125D, 0.0D) : super.getRenderOffset(p_225627_1_, p_225627_2_);
    }

    private void setModelProperties(NPCVillagerEntity p_177137_1_) {
        BipedModel.ArmPose bipedmodel$armpose = getArmPose(p_177137_1_, Hand.MAIN_HAND);
        BipedModel.ArmPose bipedmodel$armpose1 = getArmPose(p_177137_1_, Hand.OFF_HAND);
        if (bipedmodel$armpose.isTwoHanded()) {
            bipedmodel$armpose1 = p_177137_1_.getOffhandItem().isEmpty() ? BipedModel.ArmPose.EMPTY : BipedModel.ArmPose.ITEM;
        }

        this.getModel().rightArmPose = bipedmodel$armpose;
        this.getModel().leftArmPose = bipedmodel$armpose1;

    }

    public ResourceLocation getTextureLocation(NPCVillagerEntity p_110775_1_) {
//        this.customLayerRenderer.setTexture(new ResourceLocation(Utils.resourcePathBuilder(
//                "textures/entities", "sss.png")));
        return new ResourceLocation(Utils.resourcePathBuilder("textures/entities",
                p_110775_1_.getCustomSkin() + ".png"));
    }

    protected void scale(NPCVillagerEntity p_225620_1_, MatrixStack p_225620_2_, float p_225620_3_) {
        float f = 0.9375F;
        p_225620_2_.scale(0.9375F, 0.9375F, 0.9375F);
    }

    protected void renderNameTag(NPCVillagerEntity p_225629_1_, ITextComponent p_225629_2_, MatrixStack p_225629_3_, IRenderTypeBuffer p_225629_4_, int p_225629_5_) {
        super.renderNameTag(p_225629_1_, p_225629_2_, p_225629_3_, p_225629_4_, p_225629_5_);
    }

    protected void setupRotations(NPCVillagerEntity p_225621_1_, MatrixStack p_225621_2_, float p_225621_3_, float p_225621_4_, float p_225621_5_) {
        float f = p_225621_1_.getSwimAmount(p_225621_5_);
        if (p_225621_1_.isFallFlying()) {
            super.setupRotations(p_225621_1_, p_225621_2_, p_225621_3_, p_225621_4_, p_225621_5_);
            float f1 = (float) p_225621_1_.getFallFlyingTicks() + p_225621_5_;
            float f2 = MathHelper.clamp(f1 * f1 / 100.0F, 0.0F, 1.0F);
            if (!p_225621_1_.isAutoSpinAttack()) {
                p_225621_2_.mulPose(Vector3f.XP.rotationDegrees(f2 * (-90.0F - p_225621_1_.xRot)));
            }

            Vector3d vector3d = p_225621_1_.getViewVector(p_225621_5_);
            Vector3d vector3d1 = p_225621_1_.getDeltaMovement();
            double d0 = Entity.getHorizontalDistanceSqr(vector3d1);
            double d1 = Entity.getHorizontalDistanceSqr(vector3d);
            if (d0 > 0.0D && d1 > 0.0D) {
                double d2 = (vector3d1.x * vector3d.x + vector3d1.z * vector3d.z) / Math.sqrt(d0 * d1);
                double d3 = vector3d1.x * vector3d.z - vector3d1.z * vector3d.x;
                p_225621_2_.mulPose(Vector3f.YP.rotation((float) (Math.signum(d3) * Math.acos(d2))));
            }
        } else if (f > 0.0F) {
            super.setupRotations(p_225621_1_, p_225621_2_, p_225621_3_, p_225621_4_, p_225621_5_);
            float f3 = p_225621_1_.isInWater() ? -90.0F - p_225621_1_.xRot : -90.0F;
            float f4 = MathHelper.lerp(f, 0.0F, f3);
            p_225621_2_.mulPose(Vector3f.XP.rotationDegrees(f4));
            if (p_225621_1_.isVisuallySwimming()) {
                p_225621_2_.translate(0.0D, -1.0D, (double) 0.3F);
            }
        } else {
            super.setupRotations(p_225621_1_, p_225621_2_, p_225621_3_, p_225621_4_, p_225621_5_);
        }

    }

//    @Override
//    public ResourceLocation getTextureLocation(NPCVillagerEntity p_110775_1_) {
//
//        this.customLayerRenderer.setTexture(new ResourceLocation(Utils.resourcePathBuilder(
//                "textures/entities", "sss.png")));
//
////        if (NPCVillagerManager.getInstance().isVillagerTalkingtoPlayer(p_110775_1_.getId())) {
////            LOGGER.info(String.format("keeps rendering for entity: %s", p_110775_1_.toString()));
////            this.customLayerRenderer.setTexture(new ResourceLocation("ivillager:textures/entities" +
////                        "/sss.png"));
////            } else {
////            this.customLayerRenderer.setTexture(null);
////        }
////            return new ResourceLocation("ivillager:textures/entities/villager_1_love" +
////                    ".png");
//
//        return new ResourceLocation(Utils.resourcePathBuilder("textures/entities",
//                p_110775_1_.getCustomSkin()+ ".png"));
//    }
}