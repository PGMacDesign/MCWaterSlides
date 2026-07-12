package com.pgmacdesign.mcwaterslides.funnel;

/**
 * The tornado math — pure, entity-free, unit-testable. The funnel lies on its SIDE: a
 * half-open cone whose wide mouth swallows riders from a feeder slide and whose narrow
 * throat fires them out the other end, mostly sideways (the real Howlin'-Tornado layout).
 *
 * Coordinates are the funnel's own frame: {@code a} = axial distance from the EXIT plane
 * back toward the mouth (a = length at the mouth, 0 at the exit), {@code u} = horizontal
 * offset from the funnel's centerline. Two independent motions compose the ride:
 *
 *  • TRANSVERSE — a pendulum on the circular cross-section. The restoring force is the
 *    true circle tangent (∝ s/√(1−s²), s = u/r), so it diverges at the wall: riders are
 *    physically contained no matter how hot they come in, and the swish quickens as the
 *    cone narrows (ω² = swing/r).
 *  • AXIAL — a gentle water push plus the bottom line's slope carry riders toward the
 *    exit unconditionally. There is no capture state anywhere: everyone leaves.
 *
 * The exit is purely geometric — cross the exit plane and you're released with whatever
 * momentum you have. No drain, no speed gating (the old bowl's drain gate was the bug
 * factory this design replaces).
 */
public final class FunnelPhysics {
    private FunnelPhysics() {}

    /**
     * @param mouthRadius cross-section radius at the wide entry mouth (blocks)
     * @param exitRadius  cross-section radius at the narrow exit throat (blocks)
     * @param length      axial length, mouth plane → exit plane (blocks)
     * @param drop        bottom-line height loss from mouth to exit (blocks)
     * @param swing       transverse pendulum strength (blocks/tick²) — ω² = swing / r
     * @param drag        fraction of speed bled per tick (decays the swish)
     * @param axialPush   water-current accel toward the exit (blocks/tick²)
     * @param maxSpeed    horizontal speed clamp (blocks/tick)
     */
    public record Params(double mouthRadius, double exitRadius, double length, double drop,
                         double swing, double drag, double axialPush, double maxSpeed) {}

    /** Riders past this axial coordinate (in front of the exit plane) have left the funnel. */
    public static final double EXIT_MARGIN = -0.5;
    /** Exit-ward drift tops out at this fraction of maxSpeed — the swish outlives the drift,
     *  so riders cross the trough several times before the water walks them out. */
    private static final double AXIAL_FRACTION = 0.22;
    /** How much of the bottom line's slope reaches the axial drift (most of a swishing rider's
     *  slope pull is spent climbing walls, not running the axis). */
    private static final double SLOPE_GAIN = 0.03;
    /** Outward motion past this fraction of the radius bounces (damped) — the wall backstop
     *  for entries too hot for the restoring force alone. */
    private static final double WALL_BOUNCE_AT = 0.9;

    /** Cross-section radius at axial position a (clamped linear taper, exit → mouth). */
    public static double radiusAt(double a, Params p) {
        double t = clamp(a / p.length(), 0.0, 1.0);
        return p.exitRadius() + (p.mouthRadius() - p.exitRadius()) * t;
    }

    /** Trough bottom height above the exit-plane bottom at axial position a. */
    public static double bottomAt(double a, Params p) {
        return p.drop() * clamp(a / p.length(), 0.0, 1.0);
    }

    /** Riding surface height above the exit-plane bottom at (a, u) — the circular trough. */
    public static double surfaceHeight(double a, double u, Params p) {
        double r = radiusAt(a, p);
        double uu = Math.min(Math.abs(u), r * 0.98);
        return bottomAt(a, p) + r - Math.sqrt(r * r - uu * uu);
    }

    /**
     * One horizontal step in the funnel frame. Returns {newVa, newVu}; the caller advances
     * position with the new velocity (semi-implicit — keeps the oscillator stable).
     *
     * @param a  axial position (exit plane = 0, mouth = length)
     * @param u  transverse offset from the centerline
     * @param va axial velocity (positive = toward the mouth, i.e. backward)
     * @param vu transverse velocity
     */
    public static double[] step(double a, double u, double va, double vu, Params p) {
        double r = radiusAt(a, p);
        // true circular restoring force, divergent at the wall → natural containment
        double s = clamp(u / r, -0.95, 0.95);
        double restoring = p.swing() * s / Math.sqrt(1.0 - s * s);
        double nvu = (vu - restoring) * (1.0 - p.drag());
        // wall backstop: outward motion high on the wall bounces back in, damped
        if (Math.abs(u) > WALL_BOUNCE_AT * r && Math.signum(nvu) == Math.signum(u)) {
            nvu = -0.25 * nvu;
        }
        // downhill bottom line + water current, always toward the exit (negative a);
        // the drift is capped well below the swish so the crossings happen
        double slope = p.drop() / p.length();
        double nva = (va - p.axialPush() - slope * SLOPE_GAIN) * (1.0 - p.drag());
        nva = Math.max(nva, -AXIAL_FRACTION * p.maxSpeed());
        double speed = Math.hypot(nva, nvu);
        if (speed > p.maxSpeed() && speed > 1e-9) {
            double k = p.maxSpeed() / speed;
            nva *= k;
            nvu *= k;
        }
        return new double[]{nva, nvu};
    }

    /** True once a rider has crossed the exit plane — release them with their momentum. */
    public static boolean exited(double a) {
        return a < EXIT_MARGIN;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
