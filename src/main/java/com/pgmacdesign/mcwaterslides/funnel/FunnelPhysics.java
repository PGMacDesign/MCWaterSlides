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
 *  • TRANSVERSE — a true pendulum on the circular cross-section, integrated in tangent
 *    space: climbing the wall converts speed to height and descending gives it back, so a
 *    hot entry swings HIGH up the far wall instead of losing its energy (drag is the only
 *    loss). Small-angle ω² = swing/r — the swish quickens as the cone narrows.
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
     * @param swing       effective gravity along the wall (blocks/tick²) — ω² = swing / r
     * @param drag        fraction of speed bled per tick (decays the swish)
     * @param axialPush   water-current accel toward the exit (blocks/tick²)
     * @param maxSpeed    horizontal speed clamp (blocks/tick)
     */
    public record Params(double mouthRadius, double exitRadius, double length, double drop,
                         double swing, double drag, double axialPush, double maxSpeed) {}

    /** Riders past this axial coordinate (in front of the exit plane) have left the funnel. */
    public static final double EXIT_MARGIN = -0.5;
    /** Exit-ward DRIFT tops out at this fraction of maxSpeed — the swish outlives the drift,
     *  so riders cross the trough several times before the water walks them out. A rider who
     *  ENTERS faster than the cap keeps that momentum; it decays toward the cap instead of
     *  being chopped. */
    private static final double AXIAL_FRACTION = 0.18;
    /** How much of the bottom line's slope reaches the axial drift (most of a swishing rider's
     *  slope pull is spent climbing walls, not running the axis). */
    private static final double SLOPE_GAIN = 0.03;
    /** cos(φ) floor (~80°) — keeps the tangential decomposition finite at the wall top. */
    private static final double COS_FLOOR = 0.17;
    /** Pinned above ~68° and still climbing → gentle extra bleed, the soft ceiling that stands
     *  in for the open-top rim. Gentle on purpose: the swing's energy must survive the wall. */
    private static final double CEILING_S = 0.93;
    /** The rim line as a fraction of the radius — the swing never lands beyond it (just inside
     *  the shell blocks), no matter how hot the entry. */
    private static final double RIM_LIMIT = 0.95;

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
        // TRUE pendulum on the circular wall, in tangent space: climbing converts speed to
        // height and descending gives it back — the swing's energy is conserved (drag is the
        // only loss), so a hot side-entry runs high up the FAR wall instead of being eaten.
        double s = clamp(u / r, -0.985, 0.985);
        double phi = Math.asin(s);                          // angle up the wall from the trough
        double cos = Math.max(Math.cos(phi), COS_FLOOR);
        double vt = vu / cos;                               // tangential speed along the surface
        vt = (vt - p.swing() * Math.sin(phi)) * (1.0 - p.drag());
        if (Math.abs(s) > CEILING_S && Math.signum(vt) == Math.signum(u)) {
            vt *= 0.85;                                     // soft ceiling near the open rim
        }
        double nvu = vt * cos;
        // the rim is a hard line: a swing hot enough to crest it lands ON it and sheds only
        // the overshoot — the climb itself is preserved, and the return swing keeps its energy
        double rim = RIM_LIMIT * r;
        double projected = u + nvu;
        if (Math.abs(projected) > rim) {
            nvu = Math.signum(projected) * rim - u;
        }
        // downhill bottom line + water current, always toward the exit (negative a); the
        // DRIFT converges to its cap — it never chops momentum a rider brought in
        double slope = p.drop() / p.length();
        double nva = (va - p.axialPush() - slope * SLOPE_GAIN) * (1.0 - p.drag());
        double cap = -AXIAL_FRACTION * p.maxSpeed();
        if (nva < cap) {
            nva = Math.max(nva, Math.min(va * 0.98, cap));
        }
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
