package com.pgmacdesign.mcwaterslides.gametest;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.machine.JetBlock;
import com.pgmacdesign.mcwaterslides.machine.JetBlockEntity;
import com.pgmacdesign.mcwaterslides.registry.ModBlocks;
import com.pgmacdesign.mcwaterslides.slide.SlideTubeBlock;
import com.pgmacdesign.mcwaterslides.slide.TubeShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MCWaterSlides.MOD_ID)
@PrefixGameTestTemplate(false)
public class SlideTubeGameTests {

    private static void assertTubeShape(GameTestHelper helper, BlockPos pos, TubeShape expected) {
        TubeShape actual = helper.getBlockState(pos).getValue(SlideTubeBlock.SHAPE);
        if (actual != expected) {
            helper.fail("expected " + expected + " at " + pos + " but found " + actual);
        }
    }

    /** A stack of tubes computes VERTICAL; the run's ends stay vertical too. */
    @GameTest(template = "empty5", timeoutTicks = 40)
    public static void tubeStackGoesVertical(GameTestHelper helper) {
        for (int y = 1; y <= 3; y++) {
            helper.setBlock(new BlockPos(2, y, 2), ModBlocks.SLIDE_TUBES.get(null).get());
        }
        helper.succeedWhen(() -> {
            assertTubeShape(helper, new BlockPos(2, 1, 2), TubeShape.VERTICAL);
            assertTubeShape(helper, new BlockPos(2, 2, 2), TubeShape.VERTICAL);
            assertTubeShape(helper, new BlockPos(2, 3, 2), TubeShape.VERTICAL);
        });
    }

    /** Tubes and channels interconnect in one horizontal run. */
    @GameTest(template = "empty5", timeoutTicks = 40)
    public static void tubesInterconnectWithChannels(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.SLIDE_TUBES.get(null).get());
        helper.setBlock(new BlockPos(2, 1, 3), ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.succeedWhen(() -> assertTubeShape(helper, new BlockPos(2, 1, 2), TubeShape.NORTH_SOUTH));
    }

    /**
     * Tube entry geometry: the forced swim pose (0.6 box, see RidePose) fits the 12px
     * bore; a standing player (1.8) is walled out. Pins the invariant that riders enter
     * tubes and walkers don't — full player traversal can't be gametested headless
     * (player motion is client-authoritative), so geometry is the contract.
     */
    @GameTest(template = "empty5", timeoutTicks = 40)
    public static void boreAdmitsSwimBoxOnly(GameTestHelper helper) {
        BlockPos tube = new BlockPos(2, 1, 2);
        helper.setBlock(tube, ModBlocks.SLIDE_TUBES.get(null).get());
        helper.succeedWhen(() -> {
            BlockPos abs = helper.absolutePos(tube);
            var shape = helper.getLevel().getBlockState(abs).getCollisionShape(helper.getLevel(), abs);
            double floorTop = 2 / 16.0;
            var swimBox = new net.minecraft.world.phys.AABB(0.2, floorTop + 0.001, 0.2, 0.8, floorTop + 0.6, 0.8);
            var standBox = new net.minecraft.world.phys.AABB(0.2, floorTop + 0.001, 0.2, 0.8, floorTop + 1.8, 0.8);
            if (net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape,
                    net.minecraft.world.phys.shapes.Shapes.create(swimBox),
                    net.minecraft.world.phys.shapes.BooleanOp.AND)) {
                helper.fail("swim-pose box (0.6) must fit the tube bore");
            }
            if (!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape,
                    net.minecraft.world.phys.shapes.Shapes.create(standBox),
                    net.minecraft.world.phys.shapes.BooleanOp.AND)) {
                helper.fail("standing box (1.8) must NOT fit the bore");
            }
        });
    }

    /** A chicken (0.7 tall — fits the 12px bore) rides from an open channel INTO a tube run. */
    @GameTest(template = "empty5", timeoutTicks = 200)
    public static void jetPushesChickenThroughTube(GameTestHelper helper) {
        // Open channel entry (you can't drop in through a tube's lid), then enclosed tubes.
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.SLIDE_CHANNELS.get(null).get());
        for (int z = 2; z <= 4; z++) {
            helper.setBlock(new BlockPos(2, 1, z), ModBlocks.SLIDE_TUBES.get(null).get());
        }
        helper.setBlock(new BlockPos(2, 1, 0),
                ModBlocks.JET.get().defaultBlockState().setValue(JetBlock.FACING, Direction.SOUTH));
        if (!(helper.getBlockEntity(new BlockPos(2, 1, 0)) instanceof JetBlockEntity jet)) {
            throw new GameTestAssertException("jet missing");
        }
        jet.fillBuffer();

        var chicken = helper.spawnWithNoFreeWill(EntityType.CHICKEN, new BlockPos(2, 2, 1));
        helper.succeedWhen(() -> {
            // Success = the chicken is INSIDE the tube section (z >= 2 relative).
            double relZ = chicken.position().z - helper.absoluteVec(net.minecraft.world.phys.Vec3.ZERO).z;
            if (relZ < 2.3) {
                helper.fail("chicken not inside the tube yet: z=" + relZ);
            }
        });
    }

    /** An up-jet under a vertical tube shaft lifts a zombie through it (the climb story). */
    @GameTest(template = "empty5", timeoutTicks = 400)
    public static void jetLiftsZombieUpVerticalShaft(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 2, 2), ModBlocks.SLIDE_TUBES.get(null).get());
        helper.setBlock(new BlockPos(2, 3, 2), ModBlocks.SLIDE_TUBES.get(null).get());
        helper.setBlock(new BlockPos(2, 1, 2),
                ModBlocks.JET.get().defaultBlockState().setValue(JetBlock.FACING, Direction.UP));
        if (!(helper.getBlockEntity(new BlockPos(2, 1, 2)) instanceof JetBlockEntity jet)) {
            throw new GameTestAssertException("jet missing");
        }
        jet.fillBuffer();

        var zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        helper.succeedWhen(() -> {
            double relY = zombie.position().y - helper.absoluteVec(net.minecraft.world.phys.Vec3.ZERO).y;
            if (relY < 3.5) {
                helper.fail("zombie not lifted up the shaft: y=" + relY);
            }
        });
    }
}
