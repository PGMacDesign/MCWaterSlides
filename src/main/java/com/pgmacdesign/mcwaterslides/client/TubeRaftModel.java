package com.pgmacdesign.mcwaterslides.client;

import com.pgmacdesign.mcwaterslides.entity.TubeRaftEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * A flat donut: eight cuboid segments spaced around a ring (an octagon that reads as a round
 * inner tube). All segments share one UV patch so a single small texture skins the whole ring;
 * the renderer tints it (natural leather, or a dye colour).
 */
public class TubeRaftModel extends HierarchicalModel<TubeRaftEntity> {
    private static final int SEGMENTS = 8;
    private static final float RING_RADIUS = 6.0f; // pixels (16 = one block) → 0.75-block donut

    private final ModelPart root;

    public TubeRaftModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        for (int i = 0; i < SEGMENTS; i++) {
            double a = 2.0 * Math.PI * i / SEGMENTS;
            float x = (float) (Math.cos(a) * RING_RADIUS);
            float z = (float) (Math.sin(a) * RING_RADIUS);
            parts.addOrReplaceChild("seg" + i,
                    CubeListBuilder.create().texOffs(0, 0).addBox(-2.6f, -2.0f, -2.0f, 5.0f, 4.0f, 4.0f),
                    PartPose.offsetAndRotation(x, 0.0f, z, 0.0f, (float) -a, 0.0f));
        }
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(TubeRaftEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Rigid prop — no animation.
    }
}
