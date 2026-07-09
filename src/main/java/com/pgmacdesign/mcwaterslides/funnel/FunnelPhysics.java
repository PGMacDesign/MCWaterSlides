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

    /**
     * One horizontal step: a central inward pull proportional to radius (parabolic bowl ⇒ SHM)
     * plus drag, clamped to maxSpeed. Semi-implicit (caller advances position with the new
     * velocity), which keeps the oscillator stable.
     *
     * @param dx,dz  rider offset from the funnel axis
     * @param vx,vz  current horizontal velocity
     * @return the new {vx, vz}
     */
    public static double[] stepHorizontal(double dx, double dz, double vx, double vz, Params p) {
        double nvx = (vx - p.pull() * dx) * (1.0 - p.drag());
        double nvz = (vz - p.pull() * dz) * (1.0 - p.drag());
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

    /** True once the rider has spiralled/settled into the drain hole and should drop out. */
    public static boolean overDrain(double r, Params p) {
        return r <= p.drainRadius();
    }
}
