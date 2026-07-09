package com.pgmacdesign.mcwaterslides.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.entity.TubeRaftEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

public class TubeRaftRenderer extends EntityRenderer<TubeRaftEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MCWaterSlides.MOD_ID, "inner_tube"), "main");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MCWaterSlides.MOD_ID, "textures/entity/inner_tube.png");
    /** Undyed tube tint (grayscale texture × this = leather brown). */
    private static final int NATURAL = 0xFF8B5A2B;

    private final TubeRaftModel model;

    public TubeRaftRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new TubeRaftModel(context.bakeLayer(LAYER));
        this.shadowRadius = 0.6f;
    }

    @Override
    public void render(TubeRaftEntity raft, float entityYaw, float partialTicks,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        pose.translate(0.0, 0.3, 0.0); // lift the donut to the waterline
        pose.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));
        pose.scale(-1.0f, -1.0f, 1.0f); // entity-model space (Y down)
        DyeColor color = raft.getColor();
        int tint = color == null ? NATURAL : 0xFF000000 | color.getTextureDiffuseColor();
        VertexConsumer vc = buffer.getBuffer(model.renderType(TEXTURE));
        model.setupAnim(raft, 0, 0, 0, 0, 0);
        model.renderToBuffer(pose, vc, packedLight, OverlayTexture.NO_OVERLAY, tint);
        pose.popPose();
        super.render(raft, entityYaw, partialTicks, pose, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TubeRaftEntity entity) {
        return TEXTURE;
    }
}
