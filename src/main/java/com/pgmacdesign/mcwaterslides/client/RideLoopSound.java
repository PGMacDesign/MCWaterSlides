package com.pgmacdesign.mcwaterslides.client;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

import com.pgmacdesign.mcwaterslides.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * The water-rush loop that follows one rider. Volume and pitch chase the rider's speed
 * with a lerp (never pops), and when the ride ends the loop FADES out over a few ticks
 * before stopping itself — every end path (bail, brake, pool, dismount, despawn) flows
 * through the same predicate, so no loop can outlive its rider. One instance per rider,
 * enforced by {@link ClientFxHandler}'s registry.
 */
public class RideLoopSound extends AbstractTickableSoundInstance {
    private static final int FADE_TICKS = 8;
    private static final double SPEED_CAP = 22.0;

    private final Entity rider;
    private final BooleanSupplier active;
    private final DoubleSupplier speedBps;
    private int fade = FADE_TICKS;

    public RideLoopSound(Entity rider, BooleanSupplier active, DoubleSupplier speedBps) {
        super(ModSounds.RIDE_LOOP.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.rider = rider;
        this.active = active;
        this.speedBps = speedBps;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0f;
        this.x = rider.getX();
        this.y = rider.getY();
        this.z = rider.getZ();
    }

    @Override
    public void tick() {
        if (rider.isRemoved()) {
            stop();
            return;
        }
        x = rider.getX();
        y = rider.getY();
        z = rider.getZ();
        double speed = speedBps.getAsDouble();
        boolean on = active.getAsBoolean() && speed > 2.0;
        float target = on ? 0.15f + (float) Mth.clamp(speed / SPEED_CAP, 0.0, 1.0) * 0.65f : 0.0f;
        volume = Mth.lerp(0.2f, volume, target);
        pitch = 0.85f + (float) Mth.clamp(speed / SPEED_CAP, 0.0, 1.0) * 0.45f;
        if (on) {
            fade = FADE_TICKS;
        } else if (--fade <= 0 || volume < 0.015f) {
            stop();
        }
    }
}
