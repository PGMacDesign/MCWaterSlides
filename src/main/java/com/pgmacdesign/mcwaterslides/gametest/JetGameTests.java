package com.pgmacdesign.mcwaterslides.gametest;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.machine.JetBlock;
import com.pgmacdesign.mcwaterslides.machine.JetBlockEntity;
import com.pgmacdesign.mcwaterslides.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MCWaterSlides.MOD_ID)
@PrefixGameTestTemplate(false)
public class JetGameTests {

    private static JetBlockEntity placeJet(GameTestHelper helper, BlockPos pos, Direction facing, boolean powered) {
        helper.setBlock(pos, ModBlocks.JET.get().defaultBlockState().setValue(JetBlock.FACING, facing));
        if (!(helper.getBlockEntity(pos) instanceof JetBlockEntity jet)) {
            throw new GameTestAssertException("jet block entity missing");
        }
        if (powered) {
            jet.fillBuffer();
        }
        return jet;
    }

    /** A powered jet's current carries a standing villager along a flat channel. */
    @GameTest(template = "empty5", timeoutTicks = 200)
    public static void jetPushesRiderAlongChannel(GameTestHelper helper) {
        for (int z = 1; z <= 4; z++) {
            helper.setBlock(new BlockPos(2, 1, z), ModBlocks.SLIDE_CHANNELS.get(null).get());
        }
        placeJet(helper, new BlockPos(2, 1, 0), Direction.SOUTH, true);

        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(2, 2, 1));
        double startZ = villager.position().z;
        helper.succeedWhen(() -> {
            if (villager.position().z - startZ < 2.5) {
                helper.fail("villager only moved " + (villager.position().z - startZ));
            }
        });
    }

    /** No RF in the buffer = dead current = nobody moves. */
    @GameTest(template = "empty5", timeoutTicks = 100)
    public static void noPowerNoPush(GameTestHelper helper) {
        for (int z = 1; z <= 4; z++) {
            helper.setBlock(new BlockPos(2, 1, z), ModBlocks.SLIDE_CHANNELS.get(null).get());
        }
        placeJet(helper, new BlockPos(2, 1, 0), Direction.SOUTH, false);

        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(2, 2, 1));
        double startZ = villager.position().z;
        helper.runAfterDelay(60, () -> {
            if (Math.abs(villager.position().z - startZ) > 1.0) {
                helper.fail("unpowered jet moved the villager");
            } else {
                helper.succeed();
            }
        });
    }

    /** An upward jet launches a swimmer out of a water column (the geyser/uphill story). */
    @GameTest(template = "empty5", timeoutTicks = 400)
    public static void upwardJetLiftsThroughWaterColumn(GameTestHelper helper) {
        // Glass-walled 1x1 water column on top of an up-facing jet.
        for (int y = 2; y <= 3; y++) {
            for (int x = 1; x <= 3; x++) {
                for (int z = 1; z <= 3; z++) {
                    if (x == 2 && z == 2) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                    } else {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.GLASS);
                    }
                }
            }
        }
        JetBlockEntity jet = placeJet(helper, new BlockPos(2, 1, 2), Direction.UP, true);

        // A zombie SINKS in water (unlike brain-driven villagers, which float via their
        // Swim behavior even with free will removed) — so reaching the top half of the
        // column is unambiguously the jet's lift, not mob AI.
        var zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        helper.succeedWhen(() -> {
            double relY = zombie.position().y - helper.absoluteVec(net.minecraft.world.phys.Vec3.ZERO).y;
            if (relY < 3.5) {
                helper.fail("zombie not lifted: y=" + relY
                        + " energized=" + jet.isEnergized()
                        + " vy=" + zombie.getDeltaMovement().y);
            }
        });
    }

    /** Carving the channel truncates the cached field (cache coherence). */
    @GameTest(template = "empty5", timeoutTicks = 100)
    public static void fieldTruncatesWhenChannelCut(GameTestHelper helper) {
        for (int z = 1; z <= 4; z++) {
            helper.setBlock(new BlockPos(2, 1, z), ModBlocks.SLIDE_CHANNELS.get(null).get());
        }
        JetBlockEntity jet = placeJet(helper, new BlockPos(2, 1, 0), Direction.SOUTH, true);

        helper.runAfterDelay(5, () -> {
            if (!jet.field().contains(helper.absolutePos(new BlockPos(2, 1, 4)))) {
                helper.fail("field should initially reach the channel end");
            }
            helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        });
        helper.runAfterDelay(10, () -> {
            if (jet.field().contains(helper.absolutePos(new BlockPos(2, 1, 4)))) {
                helper.fail("field still reaches past the cut");
            } else {
                helper.succeed();
            }
        });
    }

    /** Redstone-disabled jets are inert: no current, no push. */
    @GameTest(template = "empty5", timeoutTicks = 100)
    public static void redstoneDisablesJet(GameTestHelper helper) {
        for (int z = 1; z <= 4; z++) {
            helper.setBlock(new BlockPos(2, 1, z), ModBlocks.SLIDE_CHANNELS.get(null).get());
        }
        placeJet(helper, new BlockPos(2, 1, 0), Direction.SOUTH, true);
        helper.setBlock(new BlockPos(3, 1, 0), Blocks.REDSTONE_BLOCK);

        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(2, 2, 1));
        double startZ = villager.position().z;
        helper.runAfterDelay(5, () -> {
            if (helper.getBlockState(new BlockPos(2, 1, 0)).getValue(JetBlock.ENABLED)) {
                helper.fail("redstone signal did not disable the jet");
            }
        });
        helper.runAfterDelay(60, () -> {
            if (Math.abs(villager.position().z - startZ) > 1.0) {
                helper.fail("disabled jet moved the villager");
            } else {
                helper.succeed();
            }
        });
    }

    /**
     * A hidden jet UNDER the channel floor pushes along the run: the field seed adopts
     * the adjacent slide cell and projects the beam as if the jet sat behind it.
     */
    @GameTest(template = "empty5", timeoutTicks = 200)
    public static void hiddenJetBelowFloorPushes(GameTestHelper helper) {
        for (int z = 1; z <= 4; z++) {
            helper.setBlock(new BlockPos(2, 2, z), ModBlocks.SLIDE_CHANNELS.get(null).get());
        }
        placeJet(helper, new BlockPos(2, 1, 2), Direction.SOUTH, true);

        var husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 3, 2));
        helper.succeedWhen(() -> {
            double relZ = husk.position().z - helper.absoluteVec(net.minecraft.world.phys.Vec3.ZERO).z;
            if (relZ < 3.5) {
                helper.fail("buried jet did not push the husk along the run: z=" + relZ);
            }
        });
    }

    /** A jet mounted BESIDE the run pushes along it too (the other hiding spot). */
    @GameTest(template = "empty5", timeoutTicks = 200)
    public static void hiddenJetBesideRunPushes(GameTestHelper helper) {
        for (int z = 1; z <= 4; z++) {
            helper.setBlock(new BlockPos(2, 1, z), ModBlocks.SLIDE_CHANNELS.get(null).get());
        }
        placeJet(helper, new BlockPos(1, 1, 2), Direction.SOUTH, true);

        var husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 2));
        helper.succeedWhen(() -> {
            double relZ = husk.position().z - helper.absoluteVec(net.minecraft.world.phys.Vec3.ZERO).z;
            if (relZ < 3.5) {
                helper.fail("side-mounted jet did not push the husk along the run: z=" + relZ);
            }
        });
    }

    /** A jet with no adjacent water or slide stays dead (nozzle-must-touch-water rule). */
    @GameTest(template = "empty5", timeoutTicks = 60)
    public static void jetWithNoWaterStaysDead(GameTestHelper helper) {
        JetBlockEntity jet = placeJet(helper, new BlockPos(2, 2, 2), Direction.SOUTH, true);
        helper.runAfterDelay(10, () -> {
            if (!jet.field().isEmpty()) {
                helper.fail("dry jet must have an empty field");
            }
            helper.succeed();
        });
    }

    /** Placing a jet against a slide projects FACING onto the run's axis (look picks the sign). */
    @GameTest(template = "empty5", timeoutTicks = 60)
    public static void placementProjectsThrustOntoRun(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.SLIDE_CHANNELS.get(null).get());

        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setYRot(0); // facing south — thrust sign should project to SOUTH
        BlockPos abs = helper.absolutePos(new BlockPos(2, 1, 2));
        var stack = new net.minecraft.world.item.ItemStack(
                com.pgmacdesign.mcwaterslides.registry.ModItems.JET.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
        // Click the run's WEST outer face: the jet lands beside the channel.
        var hit = new net.minecraft.world.phys.BlockHitResult(
                new net.minecraft.world.phys.Vec3(abs.getX(), abs.getY() + 0.5, abs.getZ() + 0.5),
                Direction.WEST, abs, false);
        stack.useOn(new net.minecraft.world.item.context.UseOnContext(
                player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));

        helper.succeedWhen(() -> {
            var placed = helper.getBlockState(new BlockPos(1, 1, 2));
            if (!(placed.getBlock() instanceof JetBlock)) {
                helper.fail("jet was not placed beside the run");
            } else if (placed.getValue(JetBlock.FACING) != Direction.SOUTH) {
                helper.fail("thrust should project onto the run axis (SOUTH), got "
                        + placed.getValue(JetBlock.FACING));
            }
        });
    }

    /**
     * Daisy-chain power: fill ONE jet and its unfed neighbors charge hop-by-hop down the
     * energy gradient — the far end of a 3-jet row ends up holding RF it was never wired for.
     */
    @GameTest(template = "empty5", timeoutTicks = 200)
    public static void jetsDaisyChainEnergy(GameTestHelper helper) {
        JetBlockEntity fed = placeJet(helper, new BlockPos(1, 1, 2), Direction.NORTH, true);
        placeJet(helper, new BlockPos(2, 1, 2), Direction.NORTH, false);
        placeJet(helper, new BlockPos(3, 1, 2), Direction.NORTH, false);
        helper.succeedWhen(() -> {
            if (!(helper.getBlockEntity(new BlockPos(3, 1, 2)) instanceof JetBlockEntity far)) {
                throw new GameTestAssertException("far jet missing");
            }
            if (far.energyHandler().getEnergyStored() > 0
                    && fed.energyHandler().getEnergyStored() > 0) {
                return;
            }
            helper.fail("chain has not relayed energy yet, far="
                    + far.energyHandler().getEnergyStored());
        });
    }
}
