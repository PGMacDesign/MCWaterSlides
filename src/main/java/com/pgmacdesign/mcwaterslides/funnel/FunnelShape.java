package com.pgmacdesign.mcwaterslides.funnel;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The auto-stamped tornado shell: a side-lying half-open cone. The core sits at the EXIT
 * throat's bottom-center; slices march back toward the wide mouth, each a ring arc one
 * block thick hugging the cone surface the physics rides. The top ~120° stays open (drop
 * in from above, sunlight on the water) and the walls alternate ceramic / accent in eight
 * 45° wedges — the classic tornado-funnel pinwheel. Both end planes stay open: the mouth
 * swallows the feeder slide's riders, the throat fires them out past the core.
 */
public final class FunnelShape {
    /** Wall shell sits this far outside the rideable surface (riders never touch collision). */
    private static final double WALL_MARGIN = 0.75;
    /** Cells above axisY + this·r are skipped — the open-top wedge (~120°). */
    private static final double OPEN_TOP = 0.5;

    /** One shell cell and which of the two pinwheel colours it wears. */
    public record ShellCell(BlockPos pos, boolean accent) {}

    private FunnelShape() {}

    /** Every cell of the shell for a core at {@code core} firing riders toward {@code facing}. */
    public static List<ShellCell> shellCells(BlockPos core, Direction facing, FunnelSize size) {
        List<ShellCell> cells = new ArrayList<>();
        Direction back = facing.getOpposite();
        Direction side = facing.getClockWise();
        double taper = (size.mouthRadius() - size.exitRadius()) / size.length();
        for (int a = 0; a <= size.length(); a++) {
            double r = size.exitRadius() + taper * a + WALL_MARGIN;
            double bottomY = core.getY() + 1 + size.drop() * a / size.length();
            double axisY = bottomY + r;
            int hMax = (int) Math.ceil(r + 0.5);
            int yLo = (int) Math.floor(bottomY - 1.2);
            int yHi = (int) Math.ceil(axisY + OPEN_TOP * r);
            for (int h = -hMax; h <= hMax; h++) {
                for (int y = yLo; y <= yHi; y++) {
                    double cy = y + 0.5;
                    if (cy > axisY + OPEN_TOP * r) {
                        continue;
                    }
                    double dist = Math.hypot(h, cy - axisY);
                    if (Math.abs(dist - r) > 0.5) {
                        continue;
                    }
                    BlockPos pos = core
                            .relative(back, a)
                            .relative(side, h)
                            .atY(y);
                    if (pos.equals(core)) {
                        continue;
                    }
                    // pinwheel wedge: angle around the axis, 0 = straight down
                    double theta = Math.atan2(h, axisY - cy);
                    int sector = (int) Math.floor((theta + Math.PI) / (Math.PI / 4.0));
                    cells.add(new ShellCell(pos, sector % 2 == 1));
                }
            }
        }
        return cells;
    }
}
