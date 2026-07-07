package com.pgmacdesign.mcwaterslides.gametest;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.machine.JetBlock;
import com.pgmacdesign.mcwaterslides.machine.JetBlockEntity;
import com.pgmacdesign.mcwaterslides.registry.ModBlocks;
import com.pgmacdesign.mcwaterslides.slide.SlideChannelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MCWaterSlides.MOD_ID)
@PrefixGameTestTemplate(false)
public class PhysicsEdgeGameTests {

    /** A jet re-pays a climb: rider ascends a slope it could never coast up. */
    @GameTest(template = "empty5", timeoutTicks = 300)
    public static void uphillJetBreakEven(GameTestHelper helper) {
        // Flat approach, then a two-step climb.
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(new BlockPos(2, 2, 3), ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(new BlockPos(2, 3, 4), ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(new BlockPos(2, 1, 0),
                ModBlocks.JET.get().defaultBlockState().setValue(JetBlock.FACING, Direction.SOUTH));
        if (!(helper.getBlockEntity(new BlockPos(2, 1, 0)) instanceof JetBlockEntity jet)) {
            throw new GameTestAssertException("jet missing");
        }
        jet.fillBuffer();

        var husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 1));
        helper.succeedWhen(() -> {
            var origin = helper.absoluteVec(net.minecraft.world.phys.Vec3.ZERO);
            double relY = husk.position().y - origin.y;
            double relZ = husk.position().z - origin.z;
            if (relY < 2.8 || relZ < 3.5) {
                helper.fail("husk has not climbed the slope: y=" + relY + " z=" + relZ);
            }
        });
    }

    /** The channel's intrinsic water is untouchable: sponges do nothing to it. */
    @GameTest(template = "empty5", timeoutTicks = 60)
    public static void spongeCannotDryChannel(GameTestHelper helper) {
        BlockPos channel = new BlockPos(2, 1, 2);
        helper.setBlock(channel, ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.SPONGE);
        helper.runAfterDelay(20, () -> {
            if (!(helper.getBlockState(channel).getBlock() instanceof SlideChannelBlock)) {
                helper.fail("sponge destroyed the channel");
            } else if (!helper.getBlockState(channel).getFluidState().isEmpty()) {
                helper.fail("channel unexpectedly has a real fluid state");
            } else {
                helper.succeed();
            }
        });
    }

    /** Pistons can shove a channel; the survivor recomputes a sane shape (no crash, no orphan state). */
    @GameTest(template = "empty5", timeoutTicks = 100)
    public static void pistonPushReflowsChannel(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.SLIDE_CHANNELS.get(null).get());
        // Piston must be ADJACENT to what it pushes (no air gaps in a push column).
        helper.setBlock(new BlockPos(1, 1, 2),
                Blocks.PISTON.defaultBlockState().setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(0, 1, 2), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(20, () -> {
            // Pushed from (2,1,2) to (3,1,2); both channels should still be channels with valid shapes.
            if (!(helper.getBlockState(new BlockPos(3, 1, 2)).getBlock() instanceof SlideChannelBlock)) {
                helper.fail("pushed channel vanished");
                return;
            }
            if (!(helper.getBlockState(new BlockPos(2, 1, 1)).getBlock() instanceof SlideChannelBlock)) {
                helper.fail("stationary channel vanished");
                return;
            }
            helper.succeed();
        });
    }
}
