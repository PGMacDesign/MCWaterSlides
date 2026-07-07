package com.pgmacdesign.mcwaterslides.gametest;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.registry.ModItems;
import com.pgmacdesign.mcwaterslides.registry.ModBlocks;
import com.pgmacdesign.mcwaterslides.slide.SlideChannelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * SlideChannelBlockItem's run-extension: clicking a channel's interior (the raycast
 * lands on the floor plate, face UP) continues the run along the look direction
 * instead of vanilla's place-on-top.
 */
@GameTestHolder(MCWaterSlides.MOD_ID)
@PrefixGameTestTemplate(false)
public class PlacementGameTests {

    private static void clickChannelTop(GameTestHelper helper, BlockPos channel) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0); // yaw 0 = facing south (+Z)
        BlockPos abs = helper.absolutePos(channel);
        Vec3 hitLoc = new Vec3(abs.getX() + 0.5, abs.getY() + 2 / 16.0, abs.getZ() + 0.5);
        ItemStack stack = new ItemStack(ModItems.SLIDE_CHANNEL_ITEMS.get(null).get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(hitLoc, Direction.UP, abs, false)));
    }

    @GameTest(template = "empty5", timeoutTicks = 60)
    public static void verticalClickExtendsTheRun(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.SLIDE_CHANNELS.get(null).get());
        clickChannelTop(helper, new BlockPos(2, 1, 1));
        helper.succeedWhen(() -> {
            if (!(helper.getBlockState(new BlockPos(2, 1, 2)).getBlock() instanceof SlideChannelBlock)) {
                helper.fail("top-face click should extend the run in the look direction");
            }
            if (helper.getBlockState(new BlockPos(2, 2, 1)).getBlock() instanceof SlideChannelBlock) {
                helper.fail("channel must not stack on top of the clicked one");
            }
        });
    }

    /** Blocked ahead: the extension steps up onto the obstacle (uphill runs grow naturally). */
    @GameTest(template = "empty5", timeoutTicks = 60)
    public static void extensionClimbsAnObstacle(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.SLIDE_CHANNELS.get(null).get());
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        clickChannelTop(helper, new BlockPos(2, 1, 1));
        helper.succeedWhen(() -> {
            if (!(helper.getBlockState(new BlockPos(2, 2, 2)).getBlock() instanceof SlideChannelBlock)) {
                helper.fail("extension should climb onto the obstacle ahead");
            }
        });
    }
}
