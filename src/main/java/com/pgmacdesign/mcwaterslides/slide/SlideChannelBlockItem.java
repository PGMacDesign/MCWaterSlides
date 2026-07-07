package com.pgmacdesign.mcwaterslides.slide;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

/**
 * Run-extending placement for channels. Their open-top U-shape means the crosshair
 * usually lands on an interior surface (the floor plate), which vanilla resolves as
 * "place above" — the stacked-on-top surprise. Instead, clicking a straight channel
 * places the next one to continue the run, deterministically by which face you hit:
 *
 * <ul>
 *   <li><b>Open end</b> (the N/S face of a N-S run): extend beyond that end.</li>
 *   <li><b>Interior floor / underside</b> (up/down face): extend along the run in the
 *       direction you're facing (never perpendicular — no more "placed behind me").</li>
 *   <li><b>Outer wall</b> (a side face): fall through to vanilla lateral placement — that's
 *       how side-by-side wide slides are laid.</li>
 * </ul>
 *
 * Corners and slopes fall through to vanilla (their run direction is ambiguous). Tubes keep
 * the vanilla BlockItem entirely — their lid-top click is how tall bores are stacked.
 */
public class SlideChannelBlockItem extends BlockItem {

    public SlideChannelBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Nullable
    @Override
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        BlockState clickedState = level.getBlockState(clicked);
        if (!(clickedState.getBlock() instanceof SlideChannelBlock)) {
            return context; // only channels extend; tubes/pools use vanilla
        }
        Direction.Axis runAxis = straightAxisOf(clickedState.getValue(SlideChannelBlock.SHAPE));
        if (runAxis == null) {
            return context; // corners / slopes: vanilla
        }
        Direction face = context.getClickedFace();
        if (face.getAxis().isHorizontal() && face.getAxis() != runAxis) {
            return context; // outer wall side click: lateral placement (wide slides)
        }
        // Open-end click extends out that end; interior click extends along the facing.
        Direction ext = face.getAxis() == runAxis ? face : projectOntoAxis(context, runAxis);
        BlockPos fwd = clicked.relative(ext);
        BlockState fwdState = level.getBlockState(fwd);
        if (fwdState.canBeReplaced(context)) {
            return BlockPlaceContext.at(context, fwd, ext);
        }
        if (fwdState.getBlock() instanceof SlideSurface) {
            return context; // already a slide ahead — don't stack, let vanilla decide
        }
        BlockPos up = fwd.above();
        if (level.getBlockState(up).canBeReplaced(context)) {
            return BlockPlaceContext.at(context, up, ext); // step up over an obstacle (uphill runs)
        }
        return context;
    }

    /** The run axis of a straight shape, or null for corners/slopes (ambiguous direction). */
    @Nullable
    private static Direction.Axis straightAxisOf(RailShape shape) {
        return switch (shape) {
            case NORTH_SOUTH -> Direction.Axis.Z;
            case EAST_WEST -> Direction.Axis.X;
            default -> null;
        };
    }

    /** The run-axis direction closest to the player's facing — extension never goes sideways. */
    private static Direction projectOntoAxis(BlockPlaceContext context, Direction.Axis axis) {
        float yaw = context.getRotation() * Mth.DEG_TO_RAD;
        double component = axis == Direction.Axis.X ? -Mth.sin(yaw) : Mth.cos(yaw);
        return Direction.fromAxisAndDirection(axis,
                component >= 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
    }
}
