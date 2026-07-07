package com.pgmacdesign.mcwaterslides.gametest;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Harness smoke test. Exists so runGameTestServer has at least one test from day one —
 * with ZERO registered tests the GameTestServer refuses to start ("No test functions
 * were given!") while the build still exits 0, which reads as a false green.
 */
@GameTestHolder(MCWaterSlides.MOD_ID)
@PrefixGameTestTemplate(false)
public class ScaffoldGameTests {

    @GameTest(template = "empty5", timeoutTicks = 40)
    public static void harnessBoots(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Blocks.WATER);
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.WATER, pos));
    }
}
