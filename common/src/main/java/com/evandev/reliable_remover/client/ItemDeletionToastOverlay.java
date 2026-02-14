package com.evandev.reliable_remover.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class ItemDeletionToastOverlay {
    private static final ResourceLocation BACKGROUND_SPRITE = new ResourceLocation("minecraft", "textures/gui/toasts.png");

    private static final long DISPLAY_DURATION = 5000L;
    private static final long FADE_DURATION = 600L;

    private static Component currentMessage;
    private static ItemStack iconStack = ItemStack.EMPTY;
    private static long showTime = -1;

    public static void show(Component message, ItemStack icon) {
        currentMessage = message;
        iconStack = icon;
        showTime = Util.getMillis();
    }

    public static void render(GuiGraphics guiGraphics) {
        if (showTime == -1 || currentMessage == null) return;

        Minecraft mc = Minecraft.getInstance();

        boolean isInventory = mc.screen instanceof InventoryScreen
                || mc.screen instanceof CreativeModeInventoryScreen;

        if (!isInventory) return;

        long currentTime = Util.getMillis();
        long age = currentTime - showTime;

        if (age >= DISPLAY_DURATION) {
            showTime = -1;
            currentMessage = null;
            iconStack = ItemStack.EMPTY;
            return;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int toastWidth = 160;
        int toastHeight = 32;
        int xPos = (screenWidth - toastWidth) / 2;

        int yPos = getYPos(age, toastHeight);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(xPos, yPos, 1000);

        guiGraphics.blit(BACKGROUND_SPRITE, 0, 0, 0, 32, toastWidth, toastHeight);

        if (!iconStack.isEmpty()) {
            guiGraphics.renderFakeItem(iconStack, 8, 8);
        }

        guiGraphics.drawString(mc.font, Component.literal("Item Deleted"), 30, 7, -11534256, false);
        guiGraphics.drawString(mc.font, currentMessage, 30, 18, -16777216, false);

        guiGraphics.pose().popPose();
    }

    private static int getYPos(long age, int toastHeight) {
        float animationProgress = 1.0f;
        if (age < FADE_DURATION) {
            animationProgress = (float) age / FADE_DURATION;
        } else if (age > DISPLAY_DURATION - FADE_DURATION) {
            animationProgress = (float) (DISPLAY_DURATION - age) / FADE_DURATION;
        }

        animationProgress = Mth.clamp(animationProgress, 0.0f, 1.0f);
        float ease = 1.0f - (float) Math.pow(1.0f - animationProgress, 3);

        int baseTopPadding = 8;
        return (int) (baseTopPadding - (toastHeight + baseTopPadding) * (1.0f - ease));
    }
}