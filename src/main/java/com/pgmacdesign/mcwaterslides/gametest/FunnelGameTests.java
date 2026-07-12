package com.pgmacdesign.mcwaterslides.gametest;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.funnel.FunnelCoreBlock;
import com.pgmacdesign.mcwaterslides.funnel.FunnelSize;
import com.pgmacdesign.mcwaterslides.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
     * The tornado's bounded-dwell invariant, end to end: a rider dropped into the cone —
     * with NO velocity at all — must get washed out past the exit plane by the axial
     * current. This is the regression test for the old bowl's two killer bugs (trapped
     * walkers, drain capture); the swish shape itself is pinned by FunnelPhysicsTest.
     */
    @GameTest(template = "empty12", timeoutTicks = 400)
    public static void funnelWashesRiderOutTheExit(GameTestHelper helper) {
        for (int x = 0; x < 12; x++) {
            for (int z = 0; z < 12; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        // core at the exit throat, cone extending south behind it (SMALL: length 7).
        // setBlock skips the item-placement hook, so invoke the shell stamp explicitly.
        BlockPos corePos = new BlockPos(5, 1, 2);
        var coreState = ModBlocks.FUNNEL_CORE.get().defaultBlockState()
                .setValue(FunnelCoreBlock.SIZE, FunnelSize.SMALL)
                .setValue(FunnelCoreBlock.FACING, Direction.NORTH);
        helper.setBlock(corePos, coreState);
        ModBlocks.FUNNEL_CORE.get().setPlacedBy(helper.getLevel(), helper.absolutePos(corePos),
                coreState, null, net.minecraft.world.item.ItemStack.EMPTY);

        // the shell must have stamped both pinwheel colours
        helper.assertBlockPresent(ModBlocks.FUNNEL_WALL.get(), new BlockPos(5, 2, 8));

        // a rider parked mid-cone with zero momentum — the water alone must carry it out
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(5, 4, 7));
        Vec3 exitPlane = helper.absoluteVec(new Vec3(5.5, 2.0, 2.5));
        Vec3 spawn = villager.position();
        helper.succeedWhen(() -> {
            // released at/past the exit plane (the funnel lets go at EXIT_MARGIN, then ground
            // friction takes over — in a real build a catch slide is waiting there), having
            // actually traveled the cone from the spawn point
            boolean out = villager.position().z < exitPlane.z - 0.2;
            if (out && villager.position().distanceTo(spawn) > 3.5) {
                return;
            }
            helper.fail("rider not washed out yet, z=" + villager.position().z);
        });
    }
}
