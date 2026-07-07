package com.pgmacdesign.mcwaterslides.gametest;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.registry.ModAttachments;
import com.pgmacdesign.mcwaterslides.registry.ModBlocks;
import com.pgmacdesign.mcwaterslides.ride.RidePose;
import com.pgmacdesign.mcwaterslides.ride.RideState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The forced-pose invariant: set while riding (or standing on a ride surface), cleared
 * exactly once on EVERY exit path. Mock players aren't level-ticked, so these drive
 * {@link RidePose#reconcile} directly — the wiring (both tickers call it every tick) is
 * trivially inspectable.
 */
@GameTestHolder(MCWaterSlides.MOD_ID)
@PrefixGameTestTemplate(false)
public class RidePoseGameTests {

    @GameTest(template = "empty5")
    public static void poseForcedWhileRiding(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        RideState state = player.getData(ModAttachments.RIDE_STATE.get());
        state.startRide(10.0, Direction.EAST);
        RidePose.reconcile(player, state);
        if (player.getForcedPose() != Pose.SWIMMING) {
            helper.fail("expected forced SWIMMING pose while riding");
        }
        helper.succeed();
    }

    /** Standing on a channel forces prone even before a ride starts — the tube-entry fix. */
    @GameTest(template = "empty5")
    public static void poseForcedStandingOnChannel(GameTestHelper helper) {
        BlockPos channel = new BlockPos(2, 1, 2);
        helper.setBlock(channel, ModBlocks.SLIDE_CHANNELS.get(null).get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos abs = helper.absolutePos(channel);
        player.setPos(abs.getX() + 0.5, abs.getY() + 0.2, abs.getZ() + 0.5);
        RideState state = player.getData(ModAttachments.RIDE_STATE.get());
        RidePose.reconcile(player, state);
        if (player.getForcedPose() != Pose.SWIMMING) {
            helper.fail("standing on a channel should force prone so you can crawl into a tube");
        }
        helper.succeed();
    }

    @GameTest(template = "empty5")
    public static void poseNotForcedIdleOffSlide(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        RideState state = player.getData(ModAttachments.RIDE_STATE.get());
        RidePose.reconcile(player, state);
        if (player.getForcedPose() != null) {
            helper.fail("a player not riding and not on a slide must not be forced prone");
        }
        helper.succeed();
    }

    /** Exit path 1: explicit ride end (bail/pool/flight all route through endRide). */
    @GameTest(template = "empty5")
    public static void poseClearedOnRideEnd(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        RideState state = player.getData(ModAttachments.RIDE_STATE.get());
        state.startRide(10.0, Direction.EAST);
        RidePose.reconcile(player, state);
        state.endRide();
        RidePose.reconcile(player, state);
        if (player.getForcedPose() != null) {
            helper.fail("forced pose must clear when the ride ends");
        }
        if (state.poseForced) {
            helper.fail("poseForced flag must clear with the pose");
        }
        helper.succeed();
    }

    /** Exit path 2: stepping off the slide while not riding clears the prone pose. */
    @GameTest(template = "empty5")
    public static void poseClearedSteppingOffSlide(GameTestHelper helper) {
        BlockPos channel = new BlockPos(2, 1, 2);
        helper.setBlock(channel, ModBlocks.SLIDE_CHANNELS.get(null).get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos abs = helper.absolutePos(channel);
        player.setPos(abs.getX() + 0.5, abs.getY() + 0.2, abs.getZ() + 0.5);
        RideState state = player.getData(ModAttachments.RIDE_STATE.get());
        RidePose.reconcile(player, state); // on the channel → prone
        player.setPos(abs.getX() + 5.5, abs.getY() + 0.2, abs.getZ() + 0.5); // walk off onto air
        RidePose.reconcile(player, state);
        if (player.getForcedPose() != null) {
            helper.fail("prone must clear once you step off the slide");
        }
        helper.succeed();
    }
}
