package com.pgmacdesign.mcwaterslides.registry;

import java.util.function.Supplier;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.ride.RideState;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MCWaterSlides.MOD_ID);

    /** Transient by design — disconnect/relog is a ride exit path (no serializer). */
    public static final Supplier<AttachmentType<RideState>> RIDE_STATE =
            ATTACHMENTS.register("ride_state", () -> AttachmentType.builder(RideState::new).build());

    private ModAttachments() {}
}
