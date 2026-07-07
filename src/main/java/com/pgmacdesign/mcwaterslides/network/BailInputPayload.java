package com.pgmacdesign.mcwaterslides.network;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: bail input for the given ride session. Inputs only — carries no momentum. */
public record BailInputPayload(int sessionId) implements CustomPacketPayload {
    public static final Type<BailInputPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MCWaterSlides.MOD_ID, "bail_input"));

    public static final StreamCodec<ByteBuf, BailInputPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BailInputPayload::sessionId,
            BailInputPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
