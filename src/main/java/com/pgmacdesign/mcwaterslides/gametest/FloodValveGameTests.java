package com.pgmacdesign.mcwaterslides.gametest;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.machine.FloodValveBlock;
import com.pgmacdesign.mcwaterslides.machine.FloodValveBlockEntity;
import com.pgmacdesign.mcwaterslides.machine.JetBlock;
import com.pgmacdesign.mcwaterslides.machine.JetBlockEntity;
import com.pgmacdesign.mcwaterslides.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MCWaterSlides.MOD_ID)
@PrefixGameTestTemplate(false)
public class FloodValveGameTests {

    /** Builds a glass box with a 2x2x2 air interior at (1..2, 1..2, 1..2); valve faces in from the west. */
    private static FloodValveBlockEntity buildTank(GameTestHelper helper, boolean sealed, boolean powered) {
        for (int x = 0; x <= 3; x++) {
            for (int y = 0; y <= 3; y++) {
                for (int z = 0; z <= 3; z++) {
                    boolean interior = x >= 1 && x <= 2 && y >= 1 && y <= 2 && z >= 1 && z <= 2;
                    if (!interior) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.GLASS);
                    }
                }
            }
        }
        if (!sealed) {
            helper.setBlock(new BlockPos(3, 2, 2), Blocks.AIR); // the hole
        }
        // valve replaces a wall block, facing east into the interior
        helper.setBlock(new BlockPos(0, 1, 1),
                ModBlocks.FLOOD_VALVE.get().defaultBlockState()
                        .setValue(FloodValveBlock.FACING, Direction.EAST)
                        .setValue(FloodValveBlock.POWERED, powered));
        if (!(helper.getBlockEntity(new BlockPos(0, 1, 1)) instanceof FloodValveBlockEntity valve)) {
            throw new GameTestAssertException("flood valve block entity missing");
        }
        valve.fillBuffer();
        return valve;
    }

    @GameTest(template = "empty5", timeoutTicks = 200)
    public static void valveFillsSealedTank(GameTestHelper helper) {
        buildTank(helper, true, true);
        helper.succeedWhen(() -> {
            for (int x = 1; x <= 2; x++) {
                for (int y = 1; y <= 2; y++) {
                    for (int z = 1; z <= 2; z++) {
                        BlockPos cell = new BlockPos(x, y, z);
                        if (!helper.getBlockState(cell).getFluidState().is(FluidTags.WATER)) {
                            helper.fail("cell " + cell + " not filled yet");
                        }
                    }
                }
            }
        });
    }

    @GameTest(template = "empty5", timeoutTicks = 100)
    public static void valveRefusesUnsealedTankAndReportsLeak(GameTestHelper helper) {
        FloodValveBlockEntity valve = buildTank(helper, false, true);
        helper.runAfterDelay(40, () -> {
            for (int x = 1; x <= 2; x++) {
                for (int y = 1; y <= 2; y++) {
                    for (int z = 1; z <= 2; z++) {
                        if (helper.getBlockState(new BlockPos(x, y, z)).getFluidState().is(FluidTags.WATER)) {
                            helper.fail("unsealed volume was filled");
                            return;
                        }
                    }
                }
            }
            if (valve.lastLeak() == null) {
                helper.fail("no leak position recorded");
            } else {
                helper.succeed();
            }
        });
    }

    @GameTest(template = "empty5", timeoutTicks = 200)
    public static void valveDrainsWhenUnpowered(GameTestHelper helper) {
        buildTank(helper, true, false);
        // pre-fill the interior by hand
        for (int x = 1; x <= 2; x++) {
            for (int y = 1; y <= 2; y++) {
                for (int z = 1; z <= 2; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }
        helper.succeedWhen(() -> {
            for (int x = 1; x <= 2; x++) {
                for (int y = 1; y <= 2; y++) {
                    for (int z = 1; z <= 2; z++) {
                        BlockPos cell = new BlockPos(x, y, z);
                        if (helper.getBlockState(cell).getFluidState().is(FluidTags.WATER)) {
                            helper.fail("cell " + cell + " not drained yet");
                        }
                    }
                }
            }
        });
    }

    /** The marquee flow at template scale: valve-filled freeform shell + jet = rideable current. */
    @GameTest(template = "empty5", timeoutTicks = 400)
    public static void freeformFilledShellCarriesRider(GameTestHelper helper) {
        // Glass trough along z (interior 1 wide, 2 tall: y1..2), open top at y3 for the drop-in.
        for (int z = 0; z <= 4; z++) {
            helper.setBlock(new BlockPos(1, 1, z), Blocks.GLASS);
            helper.setBlock(new BlockPos(3, 1, z), Blocks.GLASS);
            helper.setBlock(new BlockPos(1, 2, z), Blocks.GLASS);
            helper.setBlock(new BlockPos(3, 2, z), Blocks.GLASS);
            helper.setBlock(new BlockPos(2, 0, z), Blocks.GLASS);
        }
        helper.setBlock(new BlockPos(2, 1, 4), Blocks.GLASS);
        helper.setBlock(new BlockPos(2, 2, 4), Blocks.GLASS);
        // water floor level via direct fill (valve fill covered by its own tests);
        // the jet sits AT waterline — its nozzle must touch the water path.
        for (int z = 1; z <= 3; z++) {
            helper.setBlock(new BlockPos(2, 1, z), Blocks.WATER);
        }
        helper.setBlock(new BlockPos(2, 1, 0),
                ModBlocks.JET.get().defaultBlockState().setValue(JetBlock.FACING, Direction.SOUTH));
        if (!(helper.getBlockEntity(new BlockPos(2, 1, 0)) instanceof JetBlockEntity jet)) {
            throw new GameTestAssertException("jet missing");
        }
        jet.fillBuffer();

        var zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 1));
        helper.succeedWhen(() -> {
            double relZ = zombie.position().z - helper.absoluteVec(net.minecraft.world.phys.Vec3.ZERO).z;
            if (relZ < 3.0) {
                helper.fail("zombie not carried through the freeform water: z=" + relZ);
            }
        });
    }
}
