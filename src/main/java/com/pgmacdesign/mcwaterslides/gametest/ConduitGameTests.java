package com.pgmacdesign.mcwaterslides.gametest;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.machine.JetBlock;
import com.pgmacdesign.mcwaterslides.machine.JetBlockEntity;
import com.pgmacdesign.mcwaterslides.machine.PumpHouseBlockEntity;
import com.pgmacdesign.mcwaterslides.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MCWaterSlides.MOD_ID)
@PrefixGameTestTemplate(false)
public class ConduitGameTests {

    /** Coal → Pump House → three conduits → a distant jet energizes (M2 exit, scaled to template). */
    @GameTest(template = "empty5", timeoutTicks = 300)
    public static void conduitsCarryPowerToDistantJet(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 1, 2), ModBlocks.PUMP_HOUSE.get());
        for (int x = 1; x <= 3; x++) {
            helper.setBlock(new BlockPos(x, 1, 2), ModBlocks.WATER_CONDUIT.get());
        }
        helper.setBlock(new BlockPos(4, 1, 2),
                ModBlocks.JET.get().defaultBlockState().setValue(JetBlock.FACING, Direction.SOUTH));
        helper.setBlock(new BlockPos(4, 1, 3), ModBlocks.SLIDE_CHANNELS.get(null).get());

        if (!(helper.getBlockEntity(new BlockPos(0, 1, 2)) instanceof PumpHouseBlockEntity pump)) {
            throw new GameTestAssertException("pump house missing");
        }
        pump.addFuel(new ItemStack(Items.COAL, 1));

        helper.succeedWhen(() -> {
            if (!(helper.getBlockEntity(new BlockPos(4, 1, 2)) instanceof JetBlockEntity jet)) {
                helper.fail("jet missing");
                return;
            }
            if (!jet.isEnergized()) {
                helper.fail("jet not energized through the conduit run (jet buffer="
                        + jet.energyHandler().getEnergyStored() + ")");
            }
        });
    }
}
