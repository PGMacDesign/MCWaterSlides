package com.pgmacdesign.mcwaterslides.registry;

import java.util.function.Supplier;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The ride's audio identity — all synthesized by tools/gen_sounds.py (original content). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, MCWaterSlides.MOD_ID);

    /** Seamless water-rush bed, looped while riding; volume/pitch track momentum. */
    public static final Supplier<SoundEvent> RIDE_LOOP = register("ride_loop");
    /** Splash-down impact at the pool. */
    public static final Supplier<SoundEvent> SPLASH = register("splash");
    /** Soft whoosh when a ride starts. */
    public static final Supplier<SoundEvent> SWISH = register("swish");

    private static Supplier<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(MCWaterSlides.MOD_ID, name)));
    }

    private ModSounds() {}
}
