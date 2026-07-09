package com.pgmacdesign.mcwaterslides.funnel;

/**
 * The swirl math for a funnel bowl — pure, entity-free, unit-testable (the SlidePhysics of the
 * funnel). The bowl is a surface of revolution with a PARABOLIC profile, so a rider's radial
 * motion is simple-harmonic: the exact same integrator yields both behaviours the design calls
 * for, decided only by entry velocity —
 *
 *  • enter aimed across the middle (little tangential velocity) → oscillate wall-to-wall through
 *    the center, each pass lower (the Howlin' Tornado swish);
 *  • enter along the rim (tangential velocity) → orbit and spiral inward as drag bleeds energy,
 *    speeding up as the radius shrinks (the whirlpool).
 *
 * Both decay to the drain and drop out the bottom. All lengths are blocks, all velocities
 * blocks/tick; the caller supplies the rider's offset from the funnel axis.
 */
public final class FunnelPhysics {
    private FunnelPhysics() {}

    /**
     * @param pull        central restoring accel per tick per block of radius (bowl steepness → ω²)
     * @param drag        fraction of horizontal speed bled per tick (energy decay → spiral-in)
     * @param rimRadius   bowl radius at the rim (blocks)
     * @param bowlHeight  rim height above the drain lip (blocks)
     * @param drainRadius radius at/under which the rider drops through the center hole (blocks)
     * @param maxSpeed    horizontal speed clamp (blocks/tick) — keeps the swirl in the ride's feel
     */
    public record Params(double pull, double drag, double rimRadius, double bowlHeight,
                         double drainRadius, double maxSpeed) {}

    /** A center-crossing faster than this fraction of maxSpeed skips the drain (keeps swinging). */
    public static final double DRAIN_SPEED_FRACTION = 0.42;
    /** Extra speed bleed per tick once a rider swings past the rim — contains overshoot. */
    private static final double OVERSHOOT_DAMP = 0.80;

    /**
     * One horizontal step: a central inward pull proportional to radius (parabolic bowl ⇒ SHM)
     * plus drag, clamped to maxSpeed. Semi-implicit (caller advances position with the new
     * velocity), which keeps the oscillator stable. A rider that swings past the rim bleeds
     * extra speed so it rides up the wall and falls back instead of slamming the containment.
     *
     * @param dx,dz  rider offset from the funnel axis
     * @param vx,vz  current horizontal velocity
     * @return the new {vx, vz}
     */
    public static double[] stepHorizontal(double dx, double dz, double vx, double vz, Params p) {
        double nvx = (vx - p.pull() * dx) * (1.0 - p.drag());
        double nvz = (vz - p.pull() * dz) * (1.0 - p.drag());
        if (dx * dx + dz * dz > p.rimRadius() * p.rimRadius()) {
            nvx *= OVERSHOOT_DAMP;
            nvz *= OVERSHOOT_DAMP;
        }
        double speed = Math.sqrt(nvx * nvx + nvz * nvz);
        if (speed > p.maxSpeed() && speed > 1e-9) {
            double s = p.maxSpeed() / speed;
            nvx *= s;
            nvz *= s;
        }
        return new double[]{nvx, nvz};
    }

    /** Bowl surface height above the drain lip at radius r (parabolic; flat at center, steep at rim). */
    public static double surfaceHeight(double r, Params p) {
        double rr = Math.min(r, p.rimRadius()) / p.rimRadius();
        return p.bowlHeight() * rr * rr;
    }

    /**
     * A rider drops through the drain only when it's over the hole AND slow — a fast pass across
     * the center skips it and rides up the far wall. That's what makes riders oscillate (and
     * spiral) for several decaying passes before finally dropping out, instead of vanishing down
     * the hole the instant they first cross the middle.
     */
    public static boolean shouldDrain(double r, double horizontalSpeed, Params p) {
        return r <= p.drainRadius() && horizontalSpeed < DRAIN_SPEED_FRACTION * p.maxSpeed();
    }

    /** Geometric drain-zone test (position only), independent of speed. */
    public static boolean overDrain(double r, Params p) {
        return r <= p.drainRadius();
    }
}
