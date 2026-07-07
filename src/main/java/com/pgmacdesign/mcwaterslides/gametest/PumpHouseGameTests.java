package com.pgmacdesign.mcwaterslides.gametest;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.machine.PumpHouseBlockEntity;
import com.pgmacdesign.mcwaterslides.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MCWaterSlides.MOD_ID)
@PrefixGameTestTemplate(false)
public class PumpHouseGameTests {

    private static PumpHouseBlockEntity place(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModBlocks.PUMP_HOUSE.get());
        if (!(helper.getBlockEntity(pos) instanceof PumpHouseBlockEntity be)) {
            throw new GameTestAssertException("pump house block entity missing");
        }
        return be;
    }

    /** Coal burns at the configured rate (20 RF/t default) into the buffer. */
    @GameTest(template = "empty5", timeoutTicks = 100)
    public static void coalBurnsAtConfiguredRate(GameTestHelper helper) {
        PumpHouseBlockEntity be = place(helper, new BlockPos(2, 1, 2));
        ItemStack coal = new ItemStack(Items.COAL, 1);
        if (be.addFuel(coal) <= 0) {
            helper.fail("pump house rejected coal");
        }
        // ~40 ticks at 20 RF/t with nothing to push into → ~800 RF stored.
        helper.runAfterDelay(41, () -> {
            int stored = be.storedEnergy();
            if (stored < 600 || stored > 1000) {
                helper.fail("expected ~800 RF after 40 ticks, got " + stored);
            } else {
                helper.succeed();
            }
        });
    }

    /** Adjacent water trickles passive RF while idle; drying it stops the trickle. */
    @GameTest(template = "empty5", timeoutTicks = 200)
    public static void passiveWaterTrickle(GameTestHelper helper) {
        PumpHouseBlockEntity be = place(helper, new BlockPos(2, 1, 2));
        // contain the water so it doesn't spread away
        helper.setBlock(new BlockPos(3, 0, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.GLASS);
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.GLASS);
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.GLASS);
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.WATER);

        helper.runAfterDelay(50, () -> {
            int stored = be.storedEnergy();
            if (stored < 50) {
                helper.fail("expected a passive trickle, got only " + stored + " RF");
            } else {
                helper.succeed();
            }
        });
    }

    /** The pump house pushes into an adjacent jet's buffer, which then energizes. */
    @GameTest(template = "empty5", timeoutTicks = 200)
    public static void pumpHousePowersAdjacentJet(GameTestHelper helper) {
        for (int z = 2; z <= 4; z++) {
            helper.setBlock(new BlockPos(2, 1, z), ModBlocks.SLIDE_CHANNELS.get(null).get());
        }
        helper.setBlock(new BlockPos(2, 1, 1),
                ModBlocks.JET.get().defaultBlockState()
                        .setValue(com.pgmacdesign.mcwaterslides.machine.JetBlock.FACING,
                                net.minecraft.core.Direction.SOUTH));
        PumpHouseBlockEntity be = place(helper, new BlockPos(2, 1, 0));
        be.addFuel(new ItemStack(Items.COAL, 1));

        helper.succeedWhen(() -> {
            if (!(helper.getBlockEntity(new BlockPos(2, 1, 1))
                    instanceof com.pgmacdesign.mcwaterslides.machine.JetBlockEntity jet)) {
                helper.fail("jet missing");
                return;
            }
            if (!jet.isEnergized()) {
                helper.fail("jet not energized yet (pump=" + be.storedEnergy()
                        + " jet=" + jet.energyHandler().getEnergyStored() + ")");
            }
        });
    }
}
