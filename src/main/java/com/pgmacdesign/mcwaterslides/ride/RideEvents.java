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
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide) {
            return;
        }
        // Don't allocate ride state for the 99% of mobs that never touch a slide.
        if (!living.hasData(ModAttachments.RIDE_STATE.get()) && !RideTicker.onSlideBlock(living) && !living.isInWater()) {
            return;
        }
        RideState state = living.getData(ModAttachments.RIDE_STATE.get());
        boolean wasRiding = state.riding;

        if (living instanceof ServerPlayer player) {
            if (player.getAbilities().flying) {
                state.endRide();
                return;
            }
            RideTicker.tick(player, state, player.isShiftKeyDown(), false);
            if (state.riding && player.tickCount % SYNC_INTERVAL_TICKS == 0 || state.riding != wasRiding) {
                PacketDistributor.sendToPlayer(player,
                        new RideSyncPayload(state.sessionId, state.riding, (float) state.momentum));
            }
        } else {
            RideTicker.tick(living, state, false, true);
        }
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
