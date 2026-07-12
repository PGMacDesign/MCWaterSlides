package com.pgmacdesign.mcwaterslides;

import com.pgmacdesign.mcwaterslides.funnel.FunnelPhysics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tornado laws. The two that matter most (the old bowl broke both):
 * riders OSCILLATE — the swing decays but is never yanked to a point — and
 * riders always LEAVE — the axial push admits no capture state anywhere.
 */
class FunnelPhysicsTest {
    /** LARGE-ish cone: 5-block mouth radius, 2.5 exit, 13 long, 2 drop. */
    private static final FunnelPhysics.Params P =
            new FunnelPhysics.Params(5.0, 2.5, 13.0, 2.0, 0.06, 0.008, 0.005, 1.1);

    /** Integrate one rider; returns {a, u, va, vu} after n steps or at exit. */
    private static double[] run(double a, double u, double va, double vu, int n) {
        for (int t = 0; t < n && !FunnelPhysics.exited(a); t++) {
            double[] v = FunnelPhysics.step(a, u, va, vu, P);
            va = v[0];
            vu = v[1];
            a += va;
            u += vu;
        }
        return new double[]{a, u, va, vu};
    }

    /** A rider swung in across the trough crosses the centerline several times — the swish. */
    @Test
    void transverseEntryOscillates() {
        double a = 11, u = 0, va = 0, vu = 0.9;
        int crossings = 0;
        boolean positive = false;
        double peak = 0;
        for (int t = 0; t < 600 && !FunnelPhysics.exited(a); t++) {
            double[] v = FunnelPhysics.step(a, u, va, vu, P);
            va = v[0];
            vu = v[1];
            a += va;
            u += vu;
            peak = Math.max(peak, Math.abs(u));
            if ((u > 0) != positive) {
                crossings++;
                positive = u > 0;
            }
        }
        assertTrue(crossings >= 3, "should swing across the centerline repeatedly, got " + crossings);
        assertTrue(peak > 1.5, "the swing should climb the wall, peak=" + peak);
    }

    /** THE invariant: everyone exits. Even from a dead stop — the water always wins. */
    @Test
    void everyRiderExits() {
        for (double[] start : new double[][]{
                {13.0, 0, 0, 0},       // dead stop at the mouth
                {6.5, 3.0, 0, 0},      // parked high on a wall mid-cone
                {12.0, 0, 0.9, 0.4},   // entering aimed BACKWARD (toward the mouth)
                {11.0, -2.0, 0, -1.0}, // hot tangential entry
        }) {
            double[] end = run(start[0], start[1], start[2], start[3], 4000);
            assertTrue(FunnelPhysics.exited(end[0]),
                    "rider must wash out the exit from " + java.util.Arrays.toString(start)
                            + " but sat at a=" + end[0]);
        }
    }

    /** The circle's restoring force diverges at the wall — no entry speed escapes the trough. */
    @Test
    void wallsContainTheSwing() {
        double a = 12, u = 0, va = 0, vu = 1.1; // full-speed sideways
        for (int t = 0; t < 600 && !FunnelPhysics.exited(a); t++) {
            double[] v = FunnelPhysics.step(a, u, va, vu, P);
            va = v[0];
            vu = v[1];
            a += va;
            u += vu;
            double r = FunnelPhysics.radiusAt(Math.max(a, 0), P);
            assertTrue(Math.abs(u) < r * 1.05, "swing left the trough: u=" + u + " r=" + r);
        }
    }

    /** The swish quickens as the cone narrows: stronger restoring pull at the same offset. */
    @Test
    void swishQuickensTowardExit() {
        double[] atMouth = FunnelPhysics.step(13.0, 1.0, 0, 0, P);
        double[] atThroat = FunnelPhysics.step(0.5, 1.0, 0, 0, P);
        assertTrue(Math.abs(atThroat[1]) > Math.abs(atMouth[1]),
                "restoring pull should be stronger at the narrow end");
    }

    /** Surface geometry: trough bottom on the centerline, walls rise, mouth sits `drop` higher. */
    @Test
    void surfaceProfile() {
        assertEquals(P.drop(), FunnelPhysics.bottomAt(P.length(), P), 1e-9);
        assertEquals(0.0, FunnelPhysics.bottomAt(0, P), 1e-9);
        double center = FunnelPhysics.surfaceHeight(6.5, 0, P);
        double wall = FunnelPhysics.surfaceHeight(6.5, 3.0, P);
        assertTrue(wall > center + 1.0, "the wall should climb well above the trough bottom");
        assertEquals(P.mouthRadius(), FunnelPhysics.radiusAt(P.length(), P), 1e-9);
        assertEquals(P.exitRadius(), FunnelPhysics.radiusAt(0, P), 1e-9);
    }

    /** The speed clamp is honored — the tornado never exceeds the ride's feel. */
    @Test
    void speedIsClamped() {
        double[] v = FunnelPhysics.step(6.0, 4.0, -3.0, 3.0, P);
        assertTrue(Math.hypot(v[0], v[1]) <= P.maxSpeed() + 1e-9);
    }
}
