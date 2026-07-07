package com.pgmacdesign.mcwaterslides.ride;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

/**
 * Riders lie prone: the channel water is intrinsic (no FluidState), so vanilla never
 * swims them — without this they sprint-cycle down the slide standing up. The SWIMMING
 * pose also shrinks the box to 0.6, which is what lets riders glide into tube bores.
 *
 * Reconciled every tick from the tick path rather than set/cleared on ride events:
 * exit paths are many (bail, pool, flight, dimension change, momentum decay) and ticks
 * are guaranteed once an entity holds ride state, so convergence needs no per-path code.
 */
public final class RidePose {
    /** Momentum (b/s) above which a riding player is forced prone. */
    private static final double POSE_SPEED_THRESHOLD = 3.0;

    private RidePose() {}

    public static void reconcile(Player player, RideState state) {
        boolean want = state.riding && state.momentum > POSE_SPEED_THRESHOLD;
        if (want && !state.poseForced) {
            player.setForcedPose(Pose.SWIMMING);
            state.poseForced = true;
        } else if (!want && state.poseForced) {
            player.setForcedPose(null);
            state.poseForced = false;
        }
    }
}
