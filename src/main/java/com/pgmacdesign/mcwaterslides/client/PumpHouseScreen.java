package com.pgmacdesign.mcwaterslides.client;

import com.pgmacdesign.mcwaterslides.machine.PumpHouseMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Fill-drawn console screen (MC3DPrint's charcoal-panel look — no GUI texture needed):
 * fuel slot with a flame fill above it, a vertical RF gauge, and the live rate readout.
 */
public class PumpHouseScreen extends AbstractContainerScreen<PumpHouseMenu> {
    private static final int PANEL = 0xFF1A1F2B;
    private static final int FIELD = 0xFF10141E;
    private static final int BEVEL_DARK = 0xFF0A0D14;
    private static final int ACCENT = 0xFF3FA9E0;
    private static final int LABEL = 0xFFC0C0C8;
    private static final int FLAME = 0xFFE08A3F;
    private static final int WATER = 0xFF3F76E4;

    public PumpHouseScreen(PumpHouseMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;

        // panel + bevel frame + accent line
        graphics.fill(left, top, left + imageWidth, top + imageHeight, PANEL);
        graphics.fill(left, top, left + imageWidth, top + 1, BEVEL_DARK);
        graphics.fill(left, top, left + 1, top + imageHeight, BEVEL_DARK);
        graphics.fill(left, top + imageHeight - 1, left + imageWidth, top + imageHeight, BEVEL_DARK);
        graphics.fill(left + imageWidth - 1, top, left + imageWidth, top + imageHeight, BEVEL_DARK);
        graphics.fill(left + 4, top + 14, left + imageWidth - 4, top + 15, ACCENT);

        // player inventory wells
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slotWell(graphics, left + 8 + col * 18, top + 84 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            slotWell(graphics, left + 8 + col * 18, top + 142);
        }

        // fuel slot well + flame fill above it
        slotWell(graphics, left + 80, top + 53);
        int flameH = menu.burnTotal() > 0
                ? Math.round(13f * menu.burnRemaining() / menu.burnTotal()) : 0;
        graphics.fill(left + 82, top + 36, left + 94, top + 50, FIELD);
        if (flameH > 0) {
            graphics.fill(left + 82, top + 50 - flameH, left + 94, top + 50, FLAME);
        }

        // vertical RF gauge
        int gx = left + 152, gy = top + 20, gw = 14, gh = 58;
        graphics.fill(gx - 1, gy - 1, gx + gw + 1, gy + gh + 1, BEVEL_DARK);
        graphics.fill(gx, gy, gx + gw, gy + gh, FIELD);
        int fill = Math.round(gh * (float) menu.energy() / menu.maxEnergy());
        if (fill > 0) {
            graphics.fill(gx, gy + gh - fill, gx + gw, gy + gh, ACCENT);
        }

        // live rate readout
        int rate = menu.genRate();
        String rateText = rate <= 0 ? "idle"
                : rate + " RF/t" + (menu.burnRemaining() > 0 ? "" : " (water)");
        graphics.drawString(font, rateText, left + 20, top + 42,
                rate <= 0 ? LABEL : (menu.burnRemaining() > 0 ? FLAME : WATER), false);
        graphics.drawString(font, menu.energy() + " / " + menu.maxEnergy() + " RF",
                left + 20, top + 24, LABEL, false);
    }

    private void slotWell(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, BEVEL_DARK);
        graphics.fill(x, y, x + 16, y + 16, FIELD);
    }
}
