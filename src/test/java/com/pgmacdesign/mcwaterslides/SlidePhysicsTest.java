package com.pgmacdesign.mcwaterslides;

import com.pgmacdesign.mcwaterslides.ride.RideState;
import com.pgmacdesign.mcwaterslides.ride.SlidePhysics;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure-math momentum laws (the ticket's invariant test targets, unit level). */
class SlidePhysicsTest {
    private static final SlidePhysics.Params P = new SlidePhysics.Params(22.0, 0.01, 2.0);

    @Test
    void capIsTerminalOnEveryPath() {
        // Absurd stacked gain in one tick still lands under the cap.
        double m = SlidePhysics.tickMomentum(21.9, 1, false, 0.0, new SlidePhysics.Params(22.0, 0.0, 500.0));
        assertTrue(m <= 22.0, "cap must be the last operation, got " + m);
        // And clamping never goes below zero on hard braking.
        assertEquals(0.0, SlidePhysics.tickMomentum(0.01, -1, true, 0.0, P), 0.05);
    }

    @Test
    void dragBleedsAndBrakeBleedsFaster() {
        double coast = SlidePhysics.tickMomentum(20.0, 0, false, 0.0, P);
        double braked = SlidePhysics.tickMomentum(20.0, 0, true, 0.0, P);
        assertTrue(coast < 20.0 && coast > 19.5);
        assertTrue(braked < coast);
    }

    @Test
    void slopeExchangeIsSymmetric() {
        double down = SlidePhysics.tickMomentum(10.0, 1, false, 0.0, new SlidePhysics.Params(22.0, 0.0, 2.0));
        double up = SlidePhysics.tickMomentum(10.0, -1, false, 0.0, new SlidePhysics.Params(22.0, 0.0, 2.0));
        assertEquals(down - 10.0, 10.0 - up, 1e-9, "climb must cost exactly what descent grants");
    }

    @Test
    void slopeSigns() {
        assertEquals(-1, SlidePhysics.slopeSign(RailShape.ASCENDING_SOUTH, Direction.SOUTH));
        assertEquals(1, SlidePhysics.slopeSign(RailShape.ASCENDING_SOUTH, Direction.NORTH));
        assertEquals(0, SlidePhysics.slopeSign(RailShape.NORTH_SOUTH, Direction.NORTH));
    }

    @Test
    void cornersRedirect() {
        // NORTH_EAST corner (exits N,E): arriving southbound (through the N exit) leaves east.
        assertEquals(Direction.EAST, SlidePhysics.redirect(RailShape.NORTH_EAST, Direction.SOUTH));
        // Arriving westbound (through the E exit) leaves north.
        assertEquals(Direction.NORTH, SlidePhysics.redirect(RailShape.NORTH_EAST, Direction.WEST));
        // Straights snap sideways entries onto their axis.
        assertEquals(Direction.Axis.Z, SlidePhysics.redirect(RailShape.NORTH_SOUTH, Direction.EAST).getAxis());
    }

    @Test
    void coastDistanceFromCapIsRoughlyAHundredBlocks() {
        double m = 22.0, distance = 0;
        int ticks = 0;
        while (m >= 0.3 && ticks++ < 20_000) {
            distance += m / 20.0;
            m = SlidePhysics.tickMomentum(m, 0, false, 0.0, P);
        }
        assertTrue(distance > 90 && distance < 130, "coast was " + distance + " blocks");
    }

    @Test
    void endRideIsIdempotent() {
        RideState state = new RideState();
        state.startRide(10.0, Direction.SOUTH);
        int session = state.sessionId;
        state.endRide();
        boolean ridingAfterFirst = state.riding;
        state.endRide(); // second signal must be a no-op
        assertFalse(ridingAfterFirst);
        assertFalse(state.riding);
        assertEquals(session, state.sessionId, "a duplicate exit must not disturb the epoch");
        assertEquals(0.0, state.momentum);
    }

    @Test
    void rideSessionEpochAdvancesOnStart() {
        RideState state = new RideState();
        state.startRide(5.0, Direction.EAST);
        int first = state.sessionId;
        state.endRide();
        state.startRide(5.0, Direction.EAST);
        assertTrue(state.sessionId > first, "each ride start must advance the epoch");
    }

    @Test
    void thrustAcceleratesAndOpposingThrustDecelerates() {
        double boosted = SlidePhysics.tickMomentum(10.0, 0, false, 0.4, P);
        double opposed = SlidePhysics.tickMomentum(10.0, 0, false, -0.4, P);
        assertTrue(boosted > 10.0 && boosted < 10.5);
        assertTrue(opposed < 10.0 && opposed > 9.4);
        // Even absurd thrust lands under the cap (cap is terminal).
        assertTrue(SlidePhysics.tickMomentum(10.0, 0, false, 1000.0, P) <= P.speedCap());
    }
}
