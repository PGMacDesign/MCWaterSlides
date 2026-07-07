package com.pgmacdesign.mcwaterslides.ride;

import com.pgmacdesign.mcwaterslides.slide.SlideChannelBlock;
import com.pgmacdesign.mcwaterslides.slide.SlideTubeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Riders lie prone. The channel water is intrinsic (no FluidState), so vanilla never
 * swims them — without this they sprint-cycle down the slide standing up. The SWIMMING
 * pose also shrinks the box to 0.6, which is what lets riders fit a tube's 12px bore.
 *
 * Prone engages whenever you're riding OR simply standing on a ride surface (channel /
 * tube) — not gated on speed. That's the fix for tube entry without a jet: you'd never
 * reach the old speed threshold before the mouth, so you stayed 1.8 tall and bounced off.
 * Splash pools are excluded (you stand up in the catch). Reconciled every tick from the
 * tick path, so every exit path (walk off, bail, pool, flight) converges with no per-path code.
 */
public final class RidePose {
    private RidePose() {}

    public static void reconcile(Player player, RideState state) {
        boolean want = state.riding || onRideSurface(player);
        if (want && !state.poseForced) {
            player.setForcedPose(Pose.SWIMMING);
            state.poseForced = true;
        } else if (!want && state.poseForced) {
            player.setForcedPose(null);
            state.poseForced = false;
        }
    }

    /** Feet (or one below, for the slope seam) sit on a channel or tube — the ride surfaces. */
    private static boolean onRideSurface(Player player) {
        Level level = player.level();
        BlockPos feet = BlockPos.containing(player.position());
        return isRideSurface(level.getBlockState(feet))
                || isRideSurface(level.getBlockState(feet.below()));
    }

    private static boolean isRideSurface(BlockState state) {
        return state.getBlock() instanceof SlideChannelBlock || state.getBlock() instanceof SlideTubeBlock;
    }
}
