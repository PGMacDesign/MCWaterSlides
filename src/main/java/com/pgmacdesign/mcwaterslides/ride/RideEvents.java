package com.pgmacdesign.mcwaterslides.ride;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.network.RideSyncPayload;
import com.pgmacdesign.mcwaterslides.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-side ride wiring. Players: the server integrates state-of-record only (no motion
 * — position stays client-authoritative, which is the anti-rubber-band architecture) and
 * syncs it down. Non-player LivingEntities get real motion here (server-authoritative mobs;
 * config toggles arrive with the mob-riding ticket).
 */
@EventBusSubscriber(modid = MCWaterSlides.MOD_ID)
public final class RideEvents {
    private static final int SYNC_INTERVAL_TICKS = 10;

    private RideEvents() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity item) {
            tickItem(item);
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        // Don't allocate ride state for the 99% of mobs that never touch a slide or water.
        if (!living.hasData(ModAttachments.RIDE_STATE.get()) && !RideTicker.onSlideBlock(living)
                && !living.isInWater()) {
            return;
        }
        RideState state = living.getData(ModAttachments.RIDE_STATE.get());
        boolean wasRiding = state.riding;

        if (living instanceof ServerPlayer player) {
            if (player.getAbilities().flying) {
                // Creative flight overrides the ride — end it and stand the player up.
                state.endRide();
                RidePose.reconcile(player, state);
                return;
            }
            RideTicker.tick(player, state, player.isShiftKeyDown(), false);
            RidePose.reconcile(player, state);
            if (state.riding && player.tickCount % 20 == 0) {
                var trigger = com.pgmacdesign.mcwaterslides.advancement.ModCriteria.RIDE_STAT.get();
                trigger.trigger(player, "distance", state.distanceRidden);
                trigger.trigger(player, "enclosed_distance", state.enclosedDistance);
                if (state.momentum >= com.pgmacdesign.mcwaterslides.config.MCWaterslidesConfig.SPEED_CAP.get() - 0.1) {
                    trigger.trigger(player, "speed", state.momentum);
                }
            }
            if (state.riding && player.tickCount % SYNC_INTERVAL_TICKS == 0 || state.riding != wasRiding) {
                PacketDistributor.sendToPlayer(player,
                        new RideSyncPayload(state.sessionId, state.riding, (float) state.momentum));
            }
        } else if (com.pgmacdesign.mcwaterslides.config.MCWaterslidesConfig.MOBS_RIDE.get()) {
            RideTicker.tick(living, state, false, true);
            if (state.riding && living instanceof net.minecraft.world.entity.Mob mob) {
                // Riding mobs shouldn't fight the current with pathfinding.
                mob.getNavigation().stop();
            }
        } else {
            state.endRide();
        }
    }

    /** Items have no momentum machinery — currents just shove them (free: no surge billing). */
    private static void tickItem(net.minecraft.world.entity.item.ItemEntity item) {
        if (!com.pgmacdesign.mcwaterslides.config.MCWaterslidesConfig.ITEMS_RIDE.get()) {
            return;
        }
        var thrust = com.pgmacdesign.mcwaterslides.current.CurrentFields.sampleThrust(
                item.level(), item, com.pgmacdesign.mcwaterslides.config.MCWaterslidesConfig.JET_THRUST.get());
        if (thrust.equals(net.minecraft.world.phys.Vec3.ZERO)) {
            return;
        }
        var d = item.getDeltaMovement();
        double vx = net.minecraft.util.Mth.clamp(d.x + thrust.x / 400.0, -0.6, 0.6);
        double vy = net.minecraft.util.Mth.clamp(d.y + thrust.y / 400.0, -0.6, 0.6);
        double vz = net.minecraft.util.Mth.clamp(d.z + thrust.z / 400.0, -0.6, 0.6);
        item.setDeltaMovement(vx, vy, vz);
    }

    @SubscribeEvent
    public static void onNeighborNotify(net.neoforged.neoforge.event.level.BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof net.minecraft.world.level.Level level && !level.isClientSide) {
            com.pgmacdesign.mcwaterslides.current.CurrentFields.invalidateAt(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        // Splash pool contact negates ALL landing damage, ridden or not — including a
        // cap-speed launch off an open ramp end that happens to land in one.
        net.minecraft.core.BlockPos feet = net.minecraft.core.BlockPos.containing(entity.position());
        if (entity.level().getBlockState(feet).getBlock() instanceof com.pgmacdesign.mcwaterslides.slide.SplashPoolBlock
                || entity.level().getBlockState(feet.below()).getBlock() instanceof com.pgmacdesign.mcwaterslides.slide.SplashPoolBlock) {
            event.setDamageMultiplier(0f);
            event.setCanceled(true);
            return;
        }
        RideState state = entity.getData(ModAttachments.RIDE_STATE.get());
        if (RideTicker.immuneToFall(entity, state)) {
            event.setDamageMultiplier(0f);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        event.getEntity().getData(ModAttachments.RIDE_STATE.get()).endRide();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        event.getEntity().getData(ModAttachments.RIDE_STATE.get()).endRide();
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        player.getData(ModAttachments.RIDE_STATE.get()).endRide();
    }
}
