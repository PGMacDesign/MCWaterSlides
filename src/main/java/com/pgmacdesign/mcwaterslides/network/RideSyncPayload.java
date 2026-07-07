package com.pgmacdesign.mcwaterslides.network;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** S2C: the server's ride state of record for the receiving player. */
public record RideSyncPayload(int sessionId, boolean riding, float momentum) implements CustomPacketPayload {
    public static final Type<RideSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MCWaterSlides.MOD_ID, "ride_sync"));

    public static final StreamCodec<ByteBuf, RideSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RideSyncPayload::sessionId,
            ByteBufCodecs.BOOL, RideSyncPayload::riding,
            ByteBufCodecs.FLOAT, RideSyncPayload::momentum,
            RideSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
