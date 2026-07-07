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
