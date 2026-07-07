package com.pgmacdesign.mcwaterslides.gametest;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.config.MCWaterslidesConfig;
import com.pgmacdesign.mcwaterslides.machine.JetBlock;
import com.pgmacdesign.mcwaterslides.machine.JetBlockEntity;
import com.pgmacdesign.mcwaterslides.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MCWaterSlides.MOD_ID)
@PrefixGameTestTemplate(false)
public class RiderToggleGameTests {

    private static JetBlockEntity jetAndChannels(GameTestHelper helper) {
        for (int z = 1; z <= 4; z++) {
            helper.setBlock(new BlockPos(2, 1, z), ModBlocks.SLIDE_CHANNELS.get(null).get());
        }
        helper.setBlock(new BlockPos(2, 1, 0),
                ModBlocks.JET.get().defaultBlockState().setValue(JetBlock.FACING, Direction.SOUTH));
        if (!(helper.getBlockEntity(new BlockPos(2, 1, 0)) instanceof JetBlockEntity jet)) {
            throw new GameTestAssertException("jet missing");
        }
        jet.fillBuffer();
        return jet;
    }

    /** Dropped items are carried by the current (slides double as item transport). */
    @GameTest(template = "empty5", timeoutTicks = 200)
    public static void currentCarriesDroppedItems(GameTestHelper helper) {
        jetAndChannels(helper);
        var origin = helper.absoluteVec(net.minecraft.world.phys.Vec3.ZERO);
        ItemEntity item = new ItemEntity(helper.getLevel(),
                origin.x + 2.5, origin.y + 1.5, origin.z + 1.5, new ItemStack(Items.COPPER_INGOT, 8));
        item.setDeltaMovement(0, 0, 0);
        helper.getLevel().addFreshEntity(item);

        helper.succeedWhen(() -> {
            double relZ = item.position().z - origin.z;
            if (relZ < 3.0) {
                helper.fail("item not carried yet: z=" + relZ);
            }
        });
    }

    /** mobsRide=false → currents ignore mobs entirely (vanilla behavior).
     *  Own batch: config toggles are GLOBAL, and batches serialize — running this in the
     *  default batch flips mob riding off under every concurrently-running ride test. */
    @GameTest(template = "empty5", timeoutTicks = 120, batch = "configToggleMobs")
    public static void mobsRideToggleOff(GameTestHelper helper) {
        jetAndChannels(helper);
        MCWaterslidesConfig.MOBS_RIDE.set(false);
        var husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 1));
        double startZ = husk.position().z;
        helper.runAfterDelay(60, () -> {
            try {
                if (Math.abs(husk.position().z - startZ) > 1.0) {
                    helper.fail("mob was pushed despite mobsRide=false");
                } else {
                    helper.succeed();
                }
            } finally {
                MCWaterslidesConfig.MOBS_RIDE.set(true);
            }
        });
    }

    /** itemsRide=false → currents ignore items. Own serialized batch — see above. */
    @GameTest(template = "empty5", timeoutTicks = 120, batch = "configToggleItems")
    public static void itemsRideToggleOff(GameTestHelper helper) {
        jetAndChannels(helper);
        MCWaterslidesConfig.ITEMS_RIDE.set(false);
        var origin = helper.absoluteVec(net.minecraft.world.phys.Vec3.ZERO);
        ItemEntity item = new ItemEntity(helper.getLevel(),
                origin.x + 2.5, origin.y + 1.5, origin.z + 1.5, new ItemStack(Items.CLAY_BALL, 4));
        item.setDeltaMovement(0, 0, 0);
        helper.getLevel().addFreshEntity(item);

        helper.runAfterDelay(60, () -> {
            try {
                if (item.position().z - origin.z > 2.5) {
                    helper.fail("item was pushed despite itemsRide=false");
                } else {
                    helper.succeed();
                }
            } finally {
                MCWaterslidesConfig.ITEMS_RIDE.set(true);
            }
        });
    }
}
