package com.pgmacdesign.mcwaterslides.ride;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;

/**
 * Per-entity ride state (transient attachment — a ride never survives relog by design;
 * disconnect is an exit path, see the ticket invariants). Momentum is in blocks/second.
 *
 * Single-writer: only {@link SlidePhysics#tick} (via the side's ticker) mutates this.
 * Inputs (brake/bail) are flags consumed by the tick.
 */
public class RideState {
    public boolean riding;
    public double momentum;
    @Nullable
    public Direction travel;
    /** Increments on every ride start; stamps sync payloads (epoch check). */
    public int sessionId;
    /** Ticks spent outside slide/water while riding (ride-continuity window). */
    public int gapTicks;
    /** Bail requested this tick (consumed by the ticker). */
    public boolean bailRequested;
    /** Total blocks ridden this session (advancement fuel, server only). */
    public double distanceRidden;
    /** Blocks ridden while enclosed (tubes) this session. */
    public double enclosedDistance;
    /** True while WE hold the player's forcedPose (owned by {@link RidePose#reconcile}). */
    public boolean poseForced;

    /** Idempotent exit — the ONLY way a ride ends (invariant: exit clears exactly once). */
    public void endRide() {
        if (!riding) {
            return;
        }
        riding = false;
        momentum = 0;
        travel = null;
        gapTicks = 0;
        bailRequested = false;
        distanceRidden = 0;
        enclosedDistance = 0;
    }

    public void startRide(double initialMomentum, @Nullable Direction initialTravel) {
        sessionId++;
        riding = true;
        momentum = initialMomentum;
        travel = initialTravel;
        gapTicks = 0;
        bailRequested = false;
        distanceRidden = 0;
        enclosedDistance = 0;
    }
}
