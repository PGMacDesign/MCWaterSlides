package com.pgmacdesign.mcwaterslides.gametest;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MCWaterSlides.MOD_ID)
@PrefixGameTestTemplate(false)
public class RidePhysicsGameTests {

    /** Gravity alone starts and carries a rider down a slope — no jets required. */
    @GameTest(template = "empty5", timeoutTicks = 100)
    public static void gravityStartsARideDownSlope(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 3, 0), ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(new BlockPos(2, 2, 1), ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(new BlockPos(2, 1, 3), ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(new BlockPos(2, 1, 4), ModBlocks.SLIDE_CHANNELS.get(null).get());

        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(2, 3, 1));
        Vec3 goal = helper.absoluteVec(new Vec3(2.5, 1.5, 4.0));
        helper.succeedWhen(() -> {
            if (villager.position().distanceTo(goal) > 1.6) {
                helper.fail("villager has not slid down yet: " + villager.position());
            }
        });
    }

    /**
     * Apex reversal: a rider that descends into a valley and climbs the far wall swings
     * BACK down instead of freezing on the ramp (the half-pipe valley oscillation).
     */
    @GameTest(template = "empty5", timeoutTicks = 200)
    public static void riderSwingsBackUpAValley(GameTestHelper helper) {
        // A symmetric V along +Z: down two steps to the bottom, up two steps the far wall. The
        // descending half mirrors gravityStartsARideDownSlope exactly (spawn on the 2nd, ascending
        // block so gravity auto-starts the ride); the up-wall absorbs the drop's momentum so the
        // rider stalls near the top and swings back.
        helper.setBlock(new BlockPos(2, 2, 0), ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(new BlockPos(2, 2, 3), ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(new BlockPos(2, 3, 4), ModBlocks.SLIDE_CHANNELS.get(null).get());

        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(2, 2, 1));
        double startZ = villager.position().z;
        double[] maxAdvance = {0.0};
        helper.succeedWhen(() -> {
            double adv = villager.position().z - startZ;
            if (adv > maxAdvance[0]) {
                maxAdvance[0] = adv;
            }
            // Rode out toward the far wall (shallow descent → the 2-step climb exhausts momentum),
            // then swung back.
            if (maxAdvance[0] > 1.0 && adv < maxAdvance[0] - 0.5) {
                return;
            }
            helper.fail("no return swing yet: advance=" + adv + " max=" + maxAdvance[0]);
        });
    }

    /** A moving rider is carried along a flat channel by its own momentum. */
    @GameTest(template = "empty5", timeoutTicks = 100)
    public static void momentumCarriesRiderAlongFlat(GameTestHelper helper) {
        for (int z = 0; z <= 4; z++) {
            helper.setBlock(new BlockPos(2, 1, z), ModBlocks.SLIDE_CHANNELS.get(null).get());
        }
        // Spawn a block up and drop in — spawning at channel level wedges feet into the floor slab.
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(2, 2, 0));
        villager.setDeltaMovement(0, 0, 0.4); // ~8 b/s entry, above minStartSpeed

        double startZ = villager.position().z;
        helper.succeedWhen(() -> {
            if (Math.abs(villager.position().z - startZ) < 3.0) {
                helper.fail("villager has only moved " + Math.abs(villager.position().z - startZ) + " blocks");
            }
        });
    }
}
