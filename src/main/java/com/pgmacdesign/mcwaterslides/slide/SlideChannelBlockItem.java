package com.pgmacdesign.mcwaterslides.slide;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

/**
 * Run-aware placement for channels. Their open-top U-shape means the crosshair lands on
 * whatever sub-face happens to be under it (floor, wall top, wall side), and vanilla
 * resolves the interior-floor hit as "place above" — the stacked-on-top surprise, and the
 * source of the earlier "sometimes behind, sometimes to the side" jank.
 *
 * The fix ignores which sub-face was hit and keys entirely off the clicked channel's run
 * axis and the player's facing:
 * <ul>
 *   <li>Looking <b>down the run</b> → extend it. Grows from the channel's OPEN end (so it
 *       never places behind you), or your facing if the block is isolated.</li>
 *   <li>Looking <b>across the run</b> → place a parallel lane to that side (wide slides,
 *       which auto-merge). Works even on floating channels with no ground.</li>
 * </ul>
 * Corners/slopes and non-channel slides fall through to vanilla.
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
            return context; // only channels; tubes/pools use vanilla
        }
        Direction.Axis runAxis = straightAxisOf(clickedState.getValue(SlideChannelBlock.SHAPE));
        if (runAxis == null) {
            return context; // corners / slopes: vanilla
        }
        Direction facing = context.getHorizontalDirection();
        Direction target = facing.getAxis() == runAxis
                ? extendDirection(level, clicked, runAxis, facing) // down the run → extend
                : facing;                                          // across the run → widen
        BlockPos dest = clicked.relative(target);
        BlockState destState = level.getBlockState(dest);
        if (destState.canBeReplaced(context)) {
            return BlockPlaceContext.at(context, dest, target);
        }
        if (destState.getBlock() instanceof SlideSurface) {
            return context; // already a slide there — don't stack
        }
        BlockPos up = dest.above();
        if (level.getBlockState(up).canBeReplaced(context)) {
            return BlockPlaceContext.at(context, up, target); // step up over an obstacle (uphill runs)
        }
        return context;
    }

    /** Extend toward the channel's open end; if both/neither ends are open, use the facing. */
    private static Direction extendDirection(Level level, BlockPos clicked, Direction.Axis axis, Direction facing) {
        Direction pos = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        Direction neg = pos.getOpposite();
        boolean posOpen = !(level.getBlockState(clicked.relative(pos)).getBlock() instanceof SlideSurface);
        boolean negOpen = !(level.getBlockState(clicked.relative(neg)).getBlock() instanceof SlideSurface);
        if (posOpen != negOpen) {
            return posOpen ? pos : neg;
        }
        return facing;
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
}
