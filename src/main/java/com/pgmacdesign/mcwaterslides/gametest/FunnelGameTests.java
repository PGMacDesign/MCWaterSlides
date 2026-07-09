package com.pgmacdesign.mcwaterslides.gametest;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.funnel.FunnelCoreBlock;
import com.pgmacdesign.mcwaterslides.funnel.FunnelSize;
import com.pgmacdesign.mcwaterslides.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MCWaterSlides.MOD_ID)
@PrefixGameTestTemplate(false)
public class FunnelGameTests {

    /**
     * A funnel core registers, the ride ticker swirls a real in-world rider, and it gets gathered
     * inward to the drain — the end-to-end proof that the branch fires (the exact swirl shape is
     * pinned by FunnelPhysicsTest). SMALL fits the 5³ template.
     */
    @GameTest(template = "empty5", timeoutTicks = 200)
    public static void funnelGathersRiderToDrain(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        helper.setBlock(new BlockPos(2, 1, 2),
                ModBlocks.FUNNEL_CORE.get().defaultBlockState().setValue(FunnelCoreBlock.SIZE, FunnelSize.SMALL));

        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(3, 3, 2));
        Vec3 center = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
        helper.succeedWhen(() -> {
            double dx = villager.position().x - center.x;
            double dz = villager.position().z - center.z;
            double r = Math.sqrt(dx * dx + dz * dz);
            if (r < 0.7) {
                return;
            }
            helper.fail("funnel has not gathered the rider to the drain yet, r=" + r);
        });
    }
}
