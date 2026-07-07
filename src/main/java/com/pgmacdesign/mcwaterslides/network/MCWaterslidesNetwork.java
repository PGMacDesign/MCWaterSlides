package com.pgmacdesign.mcwaterslides.network;

import com.pgmacdesign.mcwaterslides.registry.ModAttachments;
import com.pgmacdesign.mcwaterslides.ride.RideState;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * One channel, two messages: S2C ride sync (server is the state of record; epoch-checked
 * on the client) and C2S bail input (inputs only — momentum never flows client→server).
 */
public final class MCWaterslidesNetwork {
    private static final String PROTOCOL = "1";

    private MCWaterslidesNetwork() {}

    /** Mod-bus listener (wired in the @Mod constructor). */
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL)
                .playToClient(RideSyncPayload.TYPE, RideSyncPayload.STREAM_CODEC,
                        MCWaterslidesNetwork::handleRideSync)
                .playToServer(BailInputPayload.TYPE, BailInputPayload.STREAM_CODEC,
                        MCWaterslidesNetwork::handleBail);
    }

    // Client-only class reached lazily from a playToClient handler (never loads on a server).
    private static void handleRideSync(RideSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                com.pgmacdesign.mcwaterslides.client.ClientRideHandler.applySync(payload));
    }

    private static void handleBail(BailInputPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                RideState state = player.getData(ModAttachments.RIDE_STATE);
                // Epoch check: a bail from a previous ride session is dropped.
                if (state.riding && state.sessionId == payload.sessionId()) {
                    state.bailRequested = true;
                }
            }
        });
    }
}
