package com.pgmacdesign.mcwaterslides.client;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.current.CurrentFields;
import com.pgmacdesign.mcwaterslides.registry.ModAttachments;
import com.pgmacdesign.mcwaterslides.ride.RideState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The feel layer, entirely client-side (highest multi-version churn — keep it seamed
 * here): current flow particles along energized jets, rider wake/spray scaled by speed,
 * and speed-pitched swim audio. Numbers here are feel-pass tuning knobs, not physics.
 */
@EventBusSubscriber(modid = MCWaterSlides.MOD_ID, value = Dist.CLIENT)
public final class ClientFxHandler {
    private static final RandomSource RANDOM = RandomSource.create();
    private static int swimSoundCooldown;

    private ClientFxHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused() || !(player.level() instanceof ClientLevel level)) {
            return;
        }

        // Jet current visualization: a legible stream of drift particles near the player.
        CurrentFields.forEachEnergizedNear(level, player.position(), 32, (jetPos, flow, field) -> {
            // Nozzle churn.
            if (RANDOM.nextInt(3) == 0) {
                Vec3 mouth = Vec3.atCenterOf(jetPos).add(Vec3.atLowerCornerOf(flow.getNormal()).scale(0.6));
                level.addParticle(ParticleTypes.BUBBLE_POP,
                        mouth.x + jitter(0.25), mouth.y + jitter(0.25), mouth.z + jitter(0.25),
                        flow.getStepX() * 0.3, flow.getStepY() * 0.3, flow.getStepZ() * 0.3);
            }
            // Flow streaks sampled along the field (dolphin-grace reads as rushing water).
            int samples = Math.min(3, field.size());
            for (int i = 0; i < samples; i++) {
                var cell = field.randomCell(RANDOM);
                level.addParticle(ParticleTypes.DOLPHIN,
                        cell.getX() + 0.3 + jitter(0.4), cell.getY() + 0.55 + jitter(0.2), cell.getZ() + 0.3 + jitter(0.4),
                        flow.getStepX() * 0.4, flow.getStepY() * 0.4, flow.getStepZ() * 0.4);
            }
        });

        // Rider wake + speed-pitched audio.
        RideState state = player.getData(ModAttachments.RIDE_STATE.get());
        if (state.riding && state.momentum > 2.0) {
            double intensity = state.momentum / 22.0;
            int sprayCount = 1 + (int) (intensity * 3);
            for (int i = 0; i < sprayCount; i++) {
                level.addParticle(ParticleTypes.SPLASH,
                        player.getX() + jitter(0.4), player.getY() + 0.15, player.getZ() + jitter(0.4),
                        -player.getDeltaMovement().x * 0.5, 0.1 + intensity * 0.15, -player.getDeltaMovement().z * 0.5);
            }
            if (swimSoundCooldown-- <= 0) {
                level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_SWIM, SoundSource.PLAYERS,
                        0.15f + (float) intensity * 0.35f,
                        0.8f + (float) intensity * 0.6f, false);
                swimSoundCooldown = Math.max(3, (int) (14 - intensity * 10));
            }
        }
    }

    private static double jitter(double range) {
        return (RANDOM.nextDouble() - 0.5) * 2 * range;
    }
}
