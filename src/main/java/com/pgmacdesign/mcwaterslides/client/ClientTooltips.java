package com.pgmacdesign.mcwaterslides.client;

import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * One-line "what does this do" on every mod item — the mod must be self-explanatory
 * without the Patchouli book installed. Keys live in lang (tip.mcwaterslides.*); dyed
 * channel/tube variants share their family's tip. Shift reveals the detail line.
 */
@EventBusSubscriber(modid = MCWaterSlides.MOD_ID, value = Dist.CLIENT)
public final class ClientTooltips {
    private ClientTooltips() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        String descriptionId = event.getItemStack().getItem().getDescriptionId();
        int idx = descriptionId.indexOf(MCWaterSlides.MOD_ID + ".");
        if (idx < 0) {
            return;
        }
        String key = tipKey(descriptionId.substring(idx + MCWaterSlides.MOD_ID.length() + 1));
        if (!I18n.exists(key)) {
            return;
        }
        event.getToolTip().add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        String more = key + ".more";
        if (I18n.exists(more)) {
            if (Screen.hasShiftDown()) {
                event.getToolTip().add(Component.translatable(more).withStyle(ChatFormatting.DARK_GRAY));
            } else {
                event.getToolTip().add(Component.translatable("tip." + MCWaterSlides.MOD_ID + ".hold_shift")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
        }
    }

    private static String tipKey(String id) {
        // dyed + clear variants collapse onto their family tip
        if (id.endsWith("slide_channel")) {
            id = "slide_channel";
        } else if (id.endsWith("slide_tube")) {
            id = "slide_tube";
        }
        return "tip." + MCWaterSlides.MOD_ID + "." + id;
    }
}
