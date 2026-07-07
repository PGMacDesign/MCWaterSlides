package com.pgmacdesign.mcwaterslides.gametest;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.registry.ModAttachments;
import com.pgmacdesign.mcwaterslides.ride.RidePose;
import com.pgmacdesign.mcwaterslides.ride.RideState;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The forced-pose invariant: set while riding fast, cleared exactly once on EVERY exit
 * path. Mock players aren't level-ticked, so these drive {@link RidePose#reconcile}
 * directly — the wiring (both tickers call it every tick) is trivially inspectable.
 */
@GameTestHolder(MCWaterSlides.MOD_ID)
@PrefixGameTestTemplate(false)
public class RidePoseGameTests {

    @GameTest(template = "empty5")
    public static void poseForcedWhileRidingFast(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        RideState state = player.getData(ModAttachments.RIDE_STATE.get());
        state.startRide(10.0, Direction.EAST);
        RidePose.reconcile(player, state);
        if (player.getForcedPose() != Pose.SWIMMING) {
            helper.fail("expected forced SWIMMING pose while riding at 10 b/s");
        }
        helper.succeed();
    }

    @GameTest(template = "empty5")
    public static void poseNotForcedBelowThreshold(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        RideState state = player.getData(ModAttachments.RIDE_STATE.get());
        state.startRide(2.0, Direction.EAST);
        RidePose.reconcile(player, state);
        if (player.getForcedPose() != null) {
            helper.fail("slow rides must not force a pose");
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

    /** Exit path 2: momentum decays below the threshold while still riding. */
    @GameTest(template = "empty5")
    public static void poseClearedOnSlowdown(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        RideState state = player.getData(ModAttachments.RIDE_STATE.get());
        state.startRide(10.0, Direction.EAST);
        RidePose.reconcile(player, state);
        state.momentum = 1.0;
        RidePose.reconcile(player, state);
        if (player.getForcedPose() != null) {
            helper.fail("forced pose must clear when momentum decays below threshold");
        }
        helper.succeed();
    }
}
