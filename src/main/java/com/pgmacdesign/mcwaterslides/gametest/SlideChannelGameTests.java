package com.pgmacdesign.mcwaterslides.gametest;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.registry.ModBlocks;
import com.pgmacdesign.mcwaterslides.slide.SlideChannelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MCWaterSlides.MOD_ID)
@PrefixGameTestTemplate(false)
public class SlideChannelGameTests {

    private static void assertShape(GameTestHelper helper, BlockPos pos, RailShape expected) {
        RailShape actual = helper.getBlockState(pos).getValue(SlideChannelBlock.SHAPE);
        if (actual != expected) {
            helper.fail("expected " + expected + " at " + pos + " but found " + actual);
        }
    }

    @GameTest(template = "empty5", timeoutTicks = 40)
    public static void straightRunAutoConnects(GameTestHelper helper) {
        BlockPos a = new BlockPos(2, 1, 1);
        BlockPos b = new BlockPos(2, 1, 2);
        BlockPos c = new BlockPos(2, 1, 3);
        helper.setBlock(a, ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(b, ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(c, ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.succeedWhen(() -> {
            assertShape(helper, a, RailShape.NORTH_SOUTH);
            assertShape(helper, b, RailShape.NORTH_SOUTH);
            assertShape(helper, c, RailShape.NORTH_SOUTH);
        });
    }

    @GameTest(template = "empty5", timeoutTicks = 40)
    public static void cornerAutoConnects(GameTestHelper helper) {
        BlockPos a = new BlockPos(2, 1, 1);
        BlockPos b = new BlockPos(2, 1, 2);
        BlockPos c = new BlockPos(3, 1, 2);
        helper.setBlock(a, ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(b, ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(c, ModBlocks.SLIDE_CHANNELS.get(null).get());
        // b connects north (a) + east (c) → NORTH_EAST corner.
        helper.succeedWhen(() -> assertShape(helper, b, RailShape.NORTH_EAST));
    }

    @GameTest(template = "empty5", timeoutTicks = 40)
    public static void slopeAutoConnects(GameTestHelper helper) {
        BlockPos low = new BlockPos(2, 1, 1);
        BlockPos high = new BlockPos(2, 2, 2);
        helper.setBlock(low, ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(high, ModBlocks.SLIDE_CHANNELS.get(null).get());
        // The lower block ascends toward the raised southern neighbor.
        helper.succeedWhen(() -> assertShape(helper, low, RailShape.ASCENDING_SOUTH));
    }

    @GameTest(template = "empty5", timeoutTicks = 40)
    public static void colorsInterconnect(GameTestHelper helper) {
        BlockPos a = new BlockPos(2, 1, 1);
        BlockPos b = new BlockPos(2, 1, 2);
        BlockPos c = new BlockPos(3, 1, 2);
        helper.setBlock(a, ModBlocks.SLIDE_CHANNELS.get(DyeColor.CYAN).get());
        helper.setBlock(b, ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(c, ModBlocks.SLIDE_CHANNELS.get(DyeColor.RED).get());
        helper.succeedWhen(() -> assertShape(helper, b, RailShape.NORTH_EAST));
    }

    @GameTest(template = "empty5", timeoutTicks = 40)
    public static void removalReflowsNeighbors(GameTestHelper helper) {
        BlockPos a = new BlockPos(2, 1, 1);
        BlockPos b = new BlockPos(2, 1, 2);
        BlockPos c = new BlockPos(3, 1, 2);
        helper.setBlock(a, ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(b, ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(c, ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.runAfterDelay(2, () -> {
            assertShape(helper, b, RailShape.NORTH_EAST);
            helper.destroyBlock(c);
        });
        // With the east arm gone, b straightens back onto the north-south axis.
        helper.succeedWhen(() -> assertShape(helper, b, RailShape.NORTH_SOUTH));
    }
}
