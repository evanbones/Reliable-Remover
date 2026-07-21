package com.evandev.reliable_remover.api;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

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
    public static boolean isHidden(ItemStack stack, Level level, Entity holder) {
        return RuleManager.isHidden(stack, level, holder);
    }

    public static boolean isInventoryBlocked(ItemStack stack) {
        return RuleManager.isInventoryBlocked(stack);
    }

    public static boolean isInventoryBlocked(ItemStack stack, Level level, Entity holder) {
        return RuleManager.isInventoryBlocked(stack, level, holder);
    }

    public static boolean isCreativeBlocked(ItemStack stack) {
        return RuleManager.isCreativeBlocked(stack);
    }

    public static boolean isDropsBlocked(ItemStack stack, Level level, Entity entity) {
        return RuleManager.isDropsBlocked(stack, level, entity);
    }

    public static boolean isTradeBlocked(ItemStack stack) {
        return RuleManager.isTradeBlocked(stack);
    }

    public static boolean isLootBlocked(ItemStack stack) {
        return RuleManager.isLootBlocked(stack);
    }

    public static boolean isLootBlocked(ItemStack stack, LootParams params) {
        return RuleManager.isLootBlocked(stack, params);
    }

    public static boolean isEquipmentBlocked(ItemStack stack, Level level, Entity entity) {
        return RuleManager.isEquipmentBlocked(stack, level, entity);
    }

    public static boolean isStorageBlocked(ItemStack stack, Level level) {
        return RuleManager.isStorageBlocked(stack, level);
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
     * Checks if right-click interactions with a block in the world have been blocked.
     */
    public static boolean isBlockInteractionBlocked(BlockState state, Level level, BlockPos pos, Entity entity) {
        return RuleManager.isBlockInteractionBlocked(state, level, pos, entity);
    }

    /**
     * Checks if a status effect / mob effect has been blocked.
     */
    public static boolean isEffectBlocked(Holder<MobEffect> effectHolder, Level level, Entity entity) {
        return RuleManager.isEffectBlocked(effectHolder, level, entity);
    }

    /**
     * Checks if a specific enchantment has been blocked.
     */
    public static boolean isEnchantmentBlocked(Holder<Enchantment> enchantment) {
        return RuleManager.isEnchantmentBlocked(enchantment);
    }
}