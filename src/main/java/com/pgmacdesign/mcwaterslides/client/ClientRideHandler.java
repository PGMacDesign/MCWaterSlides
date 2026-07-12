package com.pgmacdesign.mcwaterslides.client;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.network.BailInputPayload;
import com.pgmacdesign.mcwaterslides.network.RideSyncPayload;
import com.pgmacdesign.mcwaterslides.registry.ModAttachments;
import com.pgmacdesign.mcwaterslides.ride.RidePose;
import com.pgmacdesign.mcwaterslides.ride.RideState;
import com.pgmacdesign.mcwaterslides.ride.RideTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Local-player prediction: runs the same {@link RideTicker} integration as the server and
 * applies real motion — the player is client-authoritative for position within vanilla
 * tolerances, so 22 b/s never rubber-bands. Remote players/mobs never run this (server
 * motion + interpolation only). Server syncs are the state of record: session mismatches
 * are adopted wholesale, same-session drift snaps to the server value past an epsilon.
 */
@EventBusSubscriber(modid = MCWaterSlides.MOD_ID, value = Dist.CLIENT)
public final class ClientRideHandler {
    /** Same-session momentum drift (b/s) beyond which the client adopts the server value. */
    private static final double RECONCILE_EPSILON = 1.5;

    private ClientRideHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused()) {
            return;
        }
        RideState state = player.getData(ModAttachments.RIDE_STATE.get());
        if (player.isSpectator() || player.getAbilities().flying) {
            // Flight, spectator, or aboard a raft: the local player isn't the rider — keep its
            // own ride state idle (the raft's client-side sim drives motion instead).
            state.endRide();
            RidePose.reconcile(player, state);
            return;
        }

        if (state.riding && player.input.jumping) {
            BlockPos feet = BlockPos.containing(player.position());
            if (!RideTicker.isEnclosed(player.level(), feet)) {
                // Predict the bail locally; the server honors the input via the payload.
                state.bailRequested = true;
                PacketDistributor.sendToServer(new BailInputPayload(state.sessionId));
            }
        }

        RideTicker.tick(player, state, player.isShiftKeyDown(), true);
        RidePose.reconcile(player, state);

        if (state.riding) {
            // Riders glide, they don't run. The swim POSE is set, but view-bob and limb
            // swing are driven separately by horizontal speed — our velocity-push makes
            // the game think you're sprinting, so the camera bobs and the model run-cycles.
            // Zero both each tick (runs post-tick, so the reset wins before the frame renders).
            player.bob = 0.0f;
            player.walkAnimation.setSpeed(0.0f);
        }
    }

    /** S2C sync handler (invoked from the network layer on the client thread). */
    public static void applySync(RideSyncPayload payload) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        RideState state = player.getData(ModAttachments.RIDE_STATE.get());
        if (payload.sessionId() != state.sessionId) {
            // New epoch: adopt the server's session wholesale. Travel direction is
            // re-derived from velocity/shape on the next predicted tick.
            state.sessionId = payload.sessionId();
            state.riding = payload.riding();
            state.momentum = payload.momentum();
            state.gapTicks = 0;
            return;
        }
        if (!payload.riding()) {
            state.endRide();
        } else if (Math.abs(state.momentum - payload.momentum()) > RECONCILE_EPSILON) {
            state.momentum = payload.momentum();
        }
    }
}
