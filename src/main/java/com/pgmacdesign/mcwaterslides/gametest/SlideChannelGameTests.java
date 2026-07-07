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

    /**
     * Parallel lanes merge: two side-by-side north-south channels drop the shared wall
     * (WALL on the abutting side goes false), outer walls stay. Wide slides read as one
     * broad surface. Mirrors the Splash Pool's inverted-wall merge.
     */
    @GameTest(template = "empty5", timeoutTicks = 40)
    public static void parallelLanesMergeWalls(GameTestHelper helper) {
        for (int z = 1; z <= 3; z++) {
            helper.setBlock(new BlockPos(2, 1, z), ModBlocks.SLIDE_CHANNELS.get(null).get());
            helper.setBlock(new BlockPos(3, 1, z), ModBlocks.SLIDE_CHANNELS.get(null).get());
        }
        helper.succeedWhen(() -> {
            var west = helper.getBlockState(new BlockPos(2, 1, 2));
            var east = helper.getBlockState(new BlockPos(3, 1, 2));
            assertShape(helper, new BlockPos(2, 1, 2), RailShape.NORTH_SOUTH);
            // West lane: pos (east) wall drops toward its neighbor, neg (west) wall stays.
            if (west.getValue(SlideChannelBlock.WALL_POS) || !west.getValue(SlideChannelBlock.WALL_NEG)) {
                helper.fail("west lane should drop only its east wall, got neg="
                        + west.getValue(SlideChannelBlock.WALL_NEG) + " pos=" + west.getValue(SlideChannelBlock.WALL_POS));
            }
            // East lane: neg (west) wall drops, pos (east) wall stays.
            if (east.getValue(SlideChannelBlock.WALL_NEG) || !east.getValue(SlideChannelBlock.WALL_POS)) {
                helper.fail("east lane should drop only its west wall, got neg="
                        + east.getValue(SlideChannelBlock.WALL_NEG) + " pos=" + east.getValue(SlideChannelBlock.WALL_POS));
            }
            // The seam is physically open: no collision in the west lane's east-wall band.
            BlockPos abs = helper.absolutePos(new BlockPos(2, 1, 2));
            var shape = helper.getLevel().getBlockState(abs).getCollisionShape(helper.getLevel(), abs);
            var seam = new net.minecraft.world.phys.AABB(14.5 / 16.0, 2.5 / 16.0, 0.3, 15.5 / 16.0, 13.5 / 16.0, 0.7);
            if (net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape,
                    net.minecraft.world.phys.shapes.Shapes.create(seam),
                    net.minecraft.world.phys.shapes.BooleanOp.AND)) {
                helper.fail("merged seam must have no wall collision");
            }
        });
    }

    /** Breaking one lane reseals the survivor's wall (existence-based recompute). */
    @GameTest(template = "empty5", timeoutTicks = 60)
    public static void unmergeRestoresWall(GameTestHelper helper) {
        for (int z = 1; z <= 3; z++) {
            helper.setBlock(new BlockPos(2, 1, z), ModBlocks.SLIDE_CHANNELS.get(null).get());
            helper.setBlock(new BlockPos(3, 1, z), ModBlocks.SLIDE_CHANNELS.get(null).get());
        }
        helper.runAfterDelay(2, () -> {
            if (helper.getBlockState(new BlockPos(2, 1, 2)).getValue(SlideChannelBlock.WALL_POS)) {
                helper.fail("precondition: west lane should have dropped its east wall");
            }
            for (int z = 1; z <= 3; z++) {
                helper.destroyBlock(new BlockPos(3, 1, z));
            }
        });
        helper.succeedWhen(() -> {
            if (!helper.getBlockState(new BlockPos(2, 1, 2)).getValue(SlideChannelBlock.WALL_POS)) {
                helper.fail("west lane must reseal its east wall once the neighbor lane is gone");
            }
        });
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
