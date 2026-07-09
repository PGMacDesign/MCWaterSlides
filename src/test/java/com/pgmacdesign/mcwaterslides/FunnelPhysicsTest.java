package com.pgmacdesign.mcwaterslides;

import com.pgmacdesign.mcwaterslides.funnel.FunnelPhysics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The funnel swirl laws: both behaviours must fall out of the one integrator. */
class FunnelPhysicsTest {
    private static final FunnelPhysics.Params P =
            new FunnelPhysics.Params(0.03, 0.008, 4.0, 4.0, 1.0, 1.2);

    /** A rider entering aimed across the middle swings wall-to-wall THROUGH center (oscillation). */
    @Test
    void radialEntryOscillatesThroughCenter() {
        double dx = 3.5, dz = 0, vx = 0, vz = 0;
        double minDx = dx;
        for (int t = 0; t < 120; t++) {
            double[] v = FunnelPhysics.stepHorizontal(dx, dz, vx, vz, P);
            vx = v[0];
            vz = v[1];
            dx += vx;
            dz += vz;
            minDx = Math.min(minDx, dx);
        }
        assertTrue(minDx < -1.0, "should swing past center to the far wall, min dx=" + minDx);
    }

    /** A rider entering tangentially orbits and spirals inward (drag bleeds the radius down). */
    @Test
    void tangentialEntrySpiralsInward() {
        double r0 = 3.5;
        double dx = r0, dz = 0, vx = 0;
        double vz = Math.sqrt(P.pull()) * r0; // ~circular-orbit speed for the SHM bowl
        double r = r0;
        for (int t = 0; t < 400; t++) {
            double[] v = FunnelPhysics.stepHorizontal(dx, dz, vx, vz, P);
            vx = v[0];
            vz = v[1];
            dx += vx;
            dz += vz;
            r = Math.hypot(dx, dz);
            assertTrue(r < P.rimRadius() * 1.3, "orbit must stay inside the bowl, r=" + r);
        }
        assertTrue(r < r0 - 1.0, "orbit should spiral inward, r went " + r0 + " -> " + r);
    }

    /** The bowl profile: flat at the drain, full height at the rim, monotonic between. */
    @Test
    void surfaceProfileEndpoints() {
        assertEquals(0.0, FunnelPhysics.surfaceHeight(0.0, P), 1e-9);
        assertEquals(4.0, FunnelPhysics.surfaceHeight(4.0, P), 1e-9);
        assertTrue(FunnelPhysics.surfaceHeight(2.0, P) < FunnelPhysics.surfaceHeight(3.0, P));
    }

    /** The speed clamp is honored — the swirl never exceeds the ride's feel. */
    @Test
    void speedIsClamped() {
        double[] v = FunnelPhysics.stepHorizontal(4.0, 0, -5.0, 5.0, P);
        assertTrue(Math.hypot(v[0], v[1]) <= P.maxSpeed() + 1e-9);
    }

    @Test
    void restsAtDeadCenter() {
        double[] v = FunnelPhysics.stepHorizontal(0, 0, 0, 0, P);
        assertEquals(0.0, v[0], 1e-9);
        assertEquals(0.0, v[1], 1e-9);
        assertTrue(FunnelPhysics.overDrain(0.0, P));
    }

    @Test
    void fastCenterCrossingSkipsTheDrain() {
        assertFalse(FunnelPhysics.shouldDrain(0.5, 1.0, P), "a fast pass over center must not drain");
        assertTrue(FunnelPhysics.shouldDrain(0.5, 0.05, P), "slow over center drains");
        assertFalse(FunnelPhysics.shouldDrain(3.0, 0.05, P), "far from center never drains");
    }

    /** The bug fix: a rider aimed across the middle swings past center several times before it
     * finally slows enough to drop the drain — it does NOT vanish on the first crossing. */
    @Test
    void riderOscillatesSeveralTimesBeforeDraining() {
        FunnelPhysics.Params p = new FunnelPhysics.Params(0.035, 0.01, 2.6, 4.0, 1.0, 0.34);
        double dx = 2.6, dz = 0, vx = -0.25, vz = 0; // enter at the rim, aimed across
        int crossings = 0;
        boolean drained = false;
        boolean prevPositive = dx > 0;
        for (int t = 0; t < 800; t++) {
            double r = Math.hypot(dx, dz);
            if (FunnelPhysics.shouldDrain(r, Math.hypot(vx, vz), p)) {
                drained = true;
                break;
            }
            double[] v = FunnelPhysics.stepHorizontal(dx, dz, vx, vz, p);
            vx = v[0];
            vz = v[1];
            dx += vx;
            dz += vz;
            if ((dx > 0) != prevPositive) {
                crossings++;
                prevPositive = dx > 0;
            }
        }
        assertTrue(crossings >= 2, "should swing across center multiple times, got " + crossings);
        assertTrue(drained, "should eventually drain once the swing decays");
    }
}
