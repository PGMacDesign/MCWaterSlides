package com.pgmacdesign.mcwaterslides.ride;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

/**
 * The pure momentum math — no Level, no entities, unit-testable. Both the server ticker
 * and the local-player client prediction run exactly this, so the two sides can only
 * diverge by input timing, not by formula.
 */
public final class SlidePhysics {
    /** Brake (crouch) deceleration, fraction of momentum per tick. */
    public static final double BRAKE_PER_TICK = 0.08;

    private SlidePhysics() {}

    /** Tunables snapshot so the math is testable without the config system. */
    public record Params(double speedCap, double dragPerTick, double slopeExchange) {}

    /**
     * One integration step. Order is fixed: slope exchange → drag → brake → clamp.
     * The clamp is the LAST operation on every path (invariant: cap is terminal).
     *
     * @param momentum  blocks/second entering the tick
     * @param slopeSign +1 descending, -1 climbing, 0 flat
     * @return momentum for the next tick, in [0, cap]
     */
    public static double tickMomentum(double momentum, int slopeSign, boolean braking, Params p) {
        double blocksThisTick = momentum / 20.0;
        double next = momentum + slopeSign * p.slopeExchange() * blocksThisTick;
        next *= (1.0 - p.dragPerTick());
        if (braking) {
            next *= (1.0 - BRAKE_PER_TICK);
        }
        return Mth.clamp(next, 0.0, p.speedCap());
    }

    /**
     * Guide the travel direction through a channel shape. Corners redirect: entering a
     * corner through exit A leaves through exit B. Straights/slopes snap travel onto
     * their axis (keeping the current heading's sign).
     */
    public static Direction redirect(RailShape shape, Direction travel) {
        Direction[] exits = exits(shape);
        if (exits.length == 2 && !isStraight(shape)) {
            if (travel == exits[0].getOpposite()) return exits[1];
            if (travel == exits[1].getOpposite()) return exits[0];
            // Entered sideways/top: head out the exit closest to current travel.
            return travel == exits[0] || travel == exits[1] ? travel : exits[0];
        }
        Direction.Axis axis = axisOf(shape);
        if (travel.getAxis() == axis) {
            return travel;
        }
        // Fell in sideways: default to the axis's positive direction.
        return Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
    }

    /** +1 when moving down the slope, -1 when climbing it, 0 on flat shapes. */
    public static int slopeSign(RailShape shape, Direction travel) {
        Direction ascent = ascentDirection(shape);
        if (ascent == null) {
            return 0;
        }
        if (travel == ascent) return -1;
        if (travel == ascent.getOpposite()) return 1;
        return 0;
    }

    /** Velocity vector for a tick: horizontal along travel, vertical along the slope. */
    public static Vec3 velocity(double momentum, Direction travel, int slopeSign) {
        double perTick = momentum / 20.0;
        Vec3 horizontal = Vec3.atLowerCornerOf(travel.getNormal()).scale(perTick);
        // Climbing rises with the steps; descending lets gravity do the fall (smoother
        // than forcing -y into collision).
        double vy = slopeSign < 0 ? perTick : 0;
        return new Vec3(horizontal.x, vy, horizontal.z);
    }

    /** The raised end of an ascending shape, or null for flat shapes. */
    @Nullable
    public static Direction ascentDirection(RailShape shape) {
        return switch (shape) {
            case ASCENDING_NORTH -> Direction.NORTH;
            case ASCENDING_SOUTH -> Direction.SOUTH;
            case ASCENDING_EAST -> Direction.EAST;
            case ASCENDING_WEST -> Direction.WEST;
            default -> null;
        };
    }

    public static Direction[] exits(RailShape shape) {
        return switch (shape) {
            case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> new Direction[]{Direction.NORTH, Direction.SOUTH};
            case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> new Direction[]{Direction.EAST, Direction.WEST};
            case SOUTH_EAST -> new Direction[]{Direction.SOUTH, Direction.EAST};
            case SOUTH_WEST -> new Direction[]{Direction.SOUTH, Direction.WEST};
            case NORTH_WEST -> new Direction[]{Direction.NORTH, Direction.WEST};
            case NORTH_EAST -> new Direction[]{Direction.NORTH, Direction.EAST};
        };
    }

    private static boolean isStraight(RailShape shape) {
        return switch (shape) {
            case SOUTH_EAST, SOUTH_WEST, NORTH_WEST, NORTH_EAST -> false;
            default -> true;
        };
    }

    private static Direction.Axis axisOf(RailShape shape) {
        return switch (shape) {
            case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> Direction.Axis.X;
            default -> Direction.Axis.Z;
        };
    }
}
