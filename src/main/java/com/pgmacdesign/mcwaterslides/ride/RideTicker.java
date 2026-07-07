package com.pgmacdesign.mcwaterslides.ride;

import javax.annotation.Nullable;

import com.pgmacdesign.mcwaterslides.config.MCWaterslidesConfig;
import com.pgmacdesign.mcwaterslides.current.CurrentFields;
import com.pgmacdesign.mcwaterslides.slide.SlideChannelBlock;
import com.pgmacdesign.mcwaterslides.slide.SlideSurface;
import com.pgmacdesign.mcwaterslides.slide.SlideTubeBlock;
import com.pgmacdesign.mcwaterslides.slide.SplashPoolBlock;
import com.pgmacdesign.mcwaterslides.slide.TubeShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

/**
 * The one integration step both sides run: the server for every rider (state of record,
 * plus actual motion for non-players), the client for the local player only (prediction —
 * players are client-authoritative for position within vanilla tolerances, which is what
 * makes 22 b/s rubber-band-free by construction).
 */
public final class RideTicker {
    private static final double END_THRESHOLD = 0.3;

    private RideTicker() {}

    public static void tick(LivingEntity entity, RideState state, boolean braking, boolean applyMotion) {
        Level level = entity.level();
        BlockPos feet = BlockPos.containing(entity.position());
        BlockState feetState = level.getBlockState(feet);
        RailShape shape = null;
        boolean verticalTube = false;
        boolean splashPool = feetState.getBlock() instanceof SplashPoolBlock;
        if (feetState.getBlock() instanceof SlideTubeBlock) {
            TubeShape tubeShape = feetState.getValue(SlideTubeBlock.SHAPE);
            if (tubeShape == TubeShape.VERTICAL) {
                verticalTube = true;
            } else {
                shape = tubeShape.toRail();
            }
        } else if (feetState.getBlock() instanceof SlideChannelBlock) {
            shape = feetState.getValue(SlideChannelBlock.SHAPE);
        }

        // Jet thrust sampled on both sides (b/s²) — smooth prediction, no packet lag.
        Vec3 thrustVec = CurrentFields.sampleThrust(level, entity, MCWaterslidesConfig.JET_THRUST.get());
        double lift = thrustVec.y / 20.0;

        // Vertical currents (geysers, tube climbs) act on anything in the field, ridden or
        // not — a ride is a horizontal concept; lift is just water behaving strongly.
        // Gated on field membership rather than isInWater: water contact flickers at the
        // surface and traps riders in a bob loop; the field only spans water/slide cells,
        // so this launches geysers clear instead.
        if (applyMotion && lift != 0) {
            Vec3 d = entity.getDeltaMovement();
            entity.setDeltaMovement(d.x, Mth.clamp(d.y + lift, -1.2, 1.2), d.z);
        }

        if (!state.riding) {
            maybeStart(entity, state, shape, verticalTube, thrustVec);
            return;
        }

        SlidePhysics.Params params = params(shape != null);

        // Bail: consumed here, honored only where geometry allows (never latched).
        if (state.bailRequested) {
            state.bailRequested = false;
            if (!isEnclosed(level, feet)) {
                state.endRide();
                return;
            }
        }

        if (splashPool) {
            // The catch: a strong smooth brake (~1.5 blocks from cap), then the ride ends
            // and vanilla control resumes — the rider stands up in the pool.
            state.gapTicks = 0;
            state.momentum *= 0.3;
            if (state.momentum < 1.0) {
                state.endRide();
                return;
            }
            if (applyMotion && state.travel != null) {
                Vec3 v = SlidePhysics.velocity(state.momentum, state.travel, 0);
                entity.setDeltaMovement(v.x, entity.getDeltaMovement().y, v.z);
            }
            return;
        }

        if (verticalTube) {
            // Drop shafts / jet climbs: gravity or lift moves the rider; a descent banks
            // momentum at the slope-exchange rate so drops pay out at the bottom.
            state.gapTicks = 0;
            double dy = entity.getDeltaMovement().y;
            if (dy < 0) {
                SlidePhysics.Params p = params(true);
                state.momentum = Math.min(state.momentum + p.slopeExchange() * (-dy), p.speedCap());
                state.distanceRidden += -dy;
            }
            return;
        }

        if (shape != null) {
            state.gapTicks = 0;
            Direction travel = state.travel != null ? state.travel : travelFromVelocity(entity);
            travel = SlidePhysics.redirect(shape, travel);
            int slopeSign = SlidePhysics.slopeSign(shape, travel);
            state.travel = travel;
            state.momentum = SlidePhysics.tickMomentum(state.momentum, slopeSign, braking,
                    thrustAlong(thrustVec, travel), params);
            state.distanceRidden += state.momentum / 20.0;
            if (state.momentum < END_THRESHOLD) {
                state.endRide();
                return;
            }
            if (applyMotion) {
                Vec3 v = SlidePhysics.velocity(state.momentum, travel, slopeSign);
                double vy = slopeSign < 0 ? v.y : entity.getDeltaMovement().y;
                entity.setDeltaMovement(v.x, vy, v.z);
                centerInChannel(entity, feet, travel, applyMotionStrength(state.momentum));
            }
        } else if (entity.isInWater()) {
            // Freeform water: jets steer (their flow becomes travel) and momentum coasts.
            state.gapTicks = 0;
            Direction flow = dominantHorizontal(thrustVec);
            if (flow != null) {
                state.travel = flow;
            }
            double along = state.travel != null ? thrustAlong(thrustVec, state.travel) : 0;
            state.momentum = SlidePhysics.tickMomentum(state.momentum, 0, braking, along, params);
            state.distanceRidden += state.momentum / 20.0;
            if (state.momentum < END_THRESHOLD) {
                state.endRide();
                return;
            }
            if (applyMotion && state.travel != null) {
                Vec3 v = SlidePhysics.velocity(state.momentum, state.travel, 0);
                entity.setDeltaMovement(v.x, entity.getDeltaMovement().y, v.z);
            }
        } else {
            // Airborne: the continuity window is the only momentum carrier (ramp jumps).
            state.gapTicks++;
            if (state.gapTicks > MCWaterslidesConfig.RIDE_CONTINUITY_TICKS.get()) {
                state.endRide();
            }
        }
    }

    private static void maybeStart(LivingEntity entity, RideState state, @Nullable RailShape shape,
                                   boolean verticalTube, Vec3 thrustVec) {
        if (entity.isSpectator()) {
            return;
        }
        Direction flow = dominantHorizontal(thrustVec);
        if (verticalTube) {
            // Entering a shaft (falling in, or a jet pushing up) starts the ride.
            if (Math.abs(entity.getDeltaMovement().y) > 0.05 || thrustVec.y != 0) {
                state.startRide(Math.max(entity.getDeltaMovement().horizontalDistance() * 20.0, 1.0), flow);
            }
            return;
        }
        if (shape == null) {
            // Jets start rides in freeform water too (that's the enclosed-tube story).
            if (flow != null && entity.isInWater()) {
                state.startRide(Math.max(entity.getDeltaMovement().horizontalDistance() * 20.0, 1.0), flow);
            }
            return;
        }
        double entrySpeed = entity.getDeltaMovement().horizontalDistance() * 20.0;
        Direction ascent = SlidePhysics.ascentDirection(shape);
        if (entrySpeed >= MCWaterslidesConfig.MIN_START_SPEED.get()) {
            state.startRide(entrySpeed, travelFromVelocity(entity));
        } else if (ascent != null) {
            // Standing on a slope: gravity starts the ride down it.
            state.startRide(Math.max(entrySpeed, 1.0), ascent.getOpposite());
        } else if (flow != null) {
            // A powered current starts you from standstill.
            state.startRide(Math.max(entrySpeed, 1.0), flow);
        }
    }

    /** Signed thrust (b/s per tick) along the travel axis; opposing jets decelerate. */
    private static double thrustAlong(Vec3 thrustVec, Direction travel) {
        return (thrustVec.x * travel.getStepX() + thrustVec.z * travel.getStepZ()) / 20.0;
    }

    @Nullable
    private static Direction dominantHorizontal(Vec3 thrustVec) {
        if (Math.abs(thrustVec.x) < 1e-4 && Math.abs(thrustVec.z) < 1e-4) {
            return null;
        }
        if (Math.abs(thrustVec.x) >= Math.abs(thrustVec.z)) {
            return thrustVec.x >= 0 ? Direction.EAST : Direction.WEST;
        }
        return thrustVec.z >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** Cheap pre-check used to avoid allocating ride state for entities nowhere near a slide. */
    public static boolean onSlideBlock(LivingEntity entity) {
        BlockPos feet = BlockPos.containing(entity.position());
        return entity.level().getBlockState(feet).getBlock() instanceof SlideSurface;
    }

    /** Fall-damage immunity: derived, never latched — riding AND currently over slide/water. */
    public static boolean immuneToFall(LivingEntity entity, RideState state) {
        if (!state.riding) {
            return false;
        }
        BlockPos feet = BlockPos.containing(entity.position());
        return entity.level().getBlockState(feet).getBlock() instanceof SlideSurface
                || entity.isInWater();
    }

    /** Committed when something with collision sits over the rider's head (tubes, roofs). */
    public static boolean isEnclosed(Level level, BlockPos feet) {
        BlockPos head = feet.above();
        return !level.getBlockState(head).getCollisionShape(level, head).isEmpty();
    }

    private static SlidePhysics.Params params(boolean inChannel) {
        return new SlidePhysics.Params(
                MCWaterslidesConfig.SPEED_CAP.get(),
                inChannel ? MCWaterslidesConfig.CHANNEL_DRAG.get() : MCWaterslidesConfig.OPEN_WATER_DRAG.get(),
                MCWaterslidesConfig.SLOPE_EXCHANGE.get());
    }

    private static Direction travelFromVelocity(LivingEntity entity) {
        Vec3 v = entity.getDeltaMovement();
        if (Math.abs(v.x) >= Math.abs(v.z)) {
            return v.x >= 0 ? Direction.EAST : Direction.WEST;
        }
        return v.z >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** Gentle pull toward the channel centerline so corners sweep instead of scrape. */
    private static void centerInChannel(LivingEntity entity, BlockPos feet, Direction travel, double strength) {
        if (travel.getAxis() == Direction.Axis.Z) {
            double center = feet.getX() + 0.5;
            double off = center - entity.getX();
            entity.setDeltaMovement(entity.getDeltaMovement().add(off * strength, 0, 0));
        } else {
            double center = feet.getZ() + 0.5;
            double off = center - entity.getZ();
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0, off * strength));
        }
    }

    private static double applyMotionStrength(double momentum) {
        // Faster riders get pulled to center harder — keeps corners survivable at cap.
        return Math.min(0.05 + momentum / 400.0, 0.12);
    }
}
