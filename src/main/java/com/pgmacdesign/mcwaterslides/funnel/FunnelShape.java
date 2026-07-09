package com.pgmacdesign.mcwaterslides.funnel;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;

/**
 * The auto-stamped bowl: a stepped wall of revolution around the core. Each layer up is a ring at
 * a wider radius following the SAME parabola the swirl physics uses (r = wallRim·√(h/H)), so the
 * visible walls track the invisible surface riders ride on. The walls sit ~0.9 blocks OUTSIDE the
 * rideable rim so a swirling rider never collides with them; the bottom center stays open — that's
 * the drain the rider drops through into whatever exit you build below.
 */
public final class FunnelShape {
    /** Wall radius margin outside the rideable rim (keeps riders off the collision wall). */
    private static final double WALL_MARGIN = 0.9;

    private FunnelShape() {}

    /** The set of cells the funnel wall occupies, relative to and above the core position. */
    public static List<BlockPos> bowlCells(BlockPos core, FunnelSize size) {
        List<BlockPos> cells = new ArrayList<>();
        double wallRim = size.rimRadius() + WALL_MARGIN;
        int bowlHeight = size.bowlHeight();
        int maxR = (int) Math.ceil(wallRim) + 1;
        for (int layer = 1; layer <= bowlHeight; layer++) {
            double ringR = wallRim * Math.sqrt((double) layer / bowlHeight);
            for (int dx = -maxR; dx <= maxR; dx++) {
                for (int dz = -maxR; dz <= maxR; dz++) {
                    double dist = Math.sqrt((double) dx * dx + (double) dz * dz);
                    if (Math.abs(dist - ringR) <= 0.55) {
                        cells.add(core.offset(dx, layer, dz));
                    }
                }
            }
        }
        return cells;
    }
}
