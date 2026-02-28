package com.evandev.reliable_remover.api;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

public class ReliableRemoverAPI {

    /**
     * Checks if the given ItemStack has been globally hidden/removed.
     *
     * @param stack The ItemStack to check.
     * @return True if the item is configured to be removed, false otherwise.
     */
    public static boolean isItemHidden(ItemStack stack) {
        return RuleManager.isHidden(stack);
    }

    /**
     * Checks if the given ItemStack has been hidden/removed in a specific dimension.
     *
     * @param stack The ItemStack to check.
     * @param level The current level/dimension context.
     * @return True if the item is removed in this level, false otherwise.
     */
    public static boolean isItemHidden(ItemStack stack, Level level) {
        return RuleManager.isHidden(stack, level);
    }

    /**
     * Checks if the given ItemStack has been hidden/removed for a specific entity in a dimension.
     *
     * @param stack  The ItemStack to check.
     * @param level  The current level/dimension context.
     * @param holder The entity holding or interacting with the item.
     * @return True if the item is removed in this context, false otherwise.
     */
    public static boolean isItemHidden(ItemStack stack, Level level, Entity holder) {
        return RuleManager.isHidden(stack, level, holder);
    }

    /**
     * Checks if using the item to attack a target has been blocked.
     */
    public static boolean isAttackBlocked(ItemStack stack, Level level, Entity target) {
        return RuleManager.isAttackBlocked(stack, level, target);
    }

    /**
     * Checks if right-click interactions with the item have been blocked.
     */
    public static boolean isInteractionBlocked(ItemStack stack, Level level, Entity target) {
        return RuleManager.isInteractionBlocked(stack, level, target);
    }

    /**
     * Checks if trades involving this item have been blocked.
     */
    public static boolean isTradeBlocked(ItemStack stack) {
        return RuleManager.isTradeBlocked(stack);
    }

    /**
     * Checks if generating this item as loot has been blocked.
     */
    public static boolean isLootBlocked(ItemStack stack) {
        return RuleManager.isLootBlocked(stack);
    }

    /**
     * Checks if a specific enchantment has been blocked.
     */
    public static boolean isEnchantmentBlocked(Holder<Enchantment> enchantment) {
        return RuleManager.isEnchantmentBlocked(enchantment);
    }
}