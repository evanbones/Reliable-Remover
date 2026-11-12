package com.evandev.reliable_remover.config;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
//? if <1.19.3 {
/*import net.minecraft.core.HolderLookup;
*///?}
//? if >=1.21 {
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
//?} else {
/*import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
*///?}

import java.util.List;
import java.util.function.Predicate;

public class EnchantmentRules {
    public static void stripBlockedEnchantments(ItemStack stack) {
        stripBlockedEnchantments(stack, null, RandomSource.create());
    }

    public static void stripBlockedEnchantments(ItemStack stack, Level level) {
        if (level != null) {
            stripBlockedEnchantments(stack, level.registryAccess(), level.getRandom());
        } else {
            stripBlockedEnchantments(stack);
        }
    }

    public static void stripBlockedEnchantments(ItemStack stack, RegistryAccess registryAccess, RandomSource random) {
        if (stack == null || stack.isEmpty()) return;

        //? if >=1.21 {
        stripFrom(stack, DataComponents.STORED_ENCHANTMENTS, registryAccess, random);
        stripFrom(stack, DataComponents.ENCHANTMENTS, null, random);
        //?} else {
        /*CompoundTag tag = stack.getTag();
        if (tag == null) return;

        stripFrom(stack, tag, "StoredEnchantments", registryAccess, random);
        stripFrom(stack, tag, "Enchantments", null, random);
        *///?}
    }

    //? if >=1.21 {
    private static void stripFrom(ItemStack stack, DataComponentType<ItemEnchantments> type, RegistryAccess rerollFrom, RandomSource random) {
        ItemEnchantments enchantments = stack.get(type);
        if (enchantments == null) return;

        boolean changed = false;
        ItemEnchantments.Mutable validEnchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (var entry : enchantments.entrySet()) {
            if (RuleManager.isEnchantmentBlocked(stack, entry.getKey())) {
                changed = true;
            } else {
                validEnchantments.set(entry.getKey(), entry.getIntValue());
            }
        }
        if (!changed) return;

        ItemEnchantments result = validEnchantments.toImmutable();
        if (result.isEmpty() && rerollFrom != null) {
            Holder<Enchantment> rerolled = getRandomAllowedEnchantment(rerollFrom, random, null);
            if (rerolled != null) {
                validEnchantments.set(rerolled, randomLevel(rerolled, random));
                result = validEnchantments.toImmutable();
            }
        }
        if (result.isEmpty()) {
            stack.remove(type);
        } else {
            stack.set(type, result);
        }
    }
    //?} else {
    /*private static void stripFrom(ItemStack stack, CompoundTag tag, String key, RegistryAccess rerollFrom, RandomSource random) {
        if (!tag.contains(key, Tag.TAG_LIST)) return;

        ListTag enchantments = tag.getList(key, Tag.TAG_COMPOUND);
        ListTag validEnchantments = filterBlockedEnchantments(stack, enchantments);
        if (validEnchantments.size() == enchantments.size()) return;

        if (validEnchantments.isEmpty() && rerollFrom != null) {
            Holder<Enchantment> rerolled = getRandomAllowedEnchantment(rerollFrom, random, null);
            if (rerolled != null) {
                validEnchantments.add(EnchantmentHelper.storeEnchantment(EnchantmentHelper.getEnchantmentId(rerolled.value()), randomLevel(rerolled, random)));
            }
        }
        if (validEnchantments.isEmpty()) {
            stack.removeTagKey(key);
        } else {
            tag.put(key, validEnchantments);
        }
    }

    private static ListTag filterBlockedEnchantments(ItemStack stack, ListTag enchantments) {
        ListTag valid = new ListTag();
        for (int i = 0; i < enchantments.size(); i++) {
            CompoundTag entry = enchantments.getCompound(i);
            if (!RuleManager.isEnchantmentBlocked(stack, getEnchantmentHolder(entry))) {
                valid.add(entry);
            }
        }
        return valid;
    }

    public static Holder<Enchantment> getEnchantmentHolder(CompoundTag enchantmentTag) {
        Identifier id = EnchantmentHelper.getEnchantmentId(enchantmentTag);
        if (id == null) return null;
        return BuiltInRegistries.ENCHANTMENT.getHolder(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, id)).orElse(null);
    }
    *///?}

    private static int randomLevel(Holder<Enchantment> enchantment, RandomSource random) {
        return Mth.nextInt(random, enchantment.value().getMinLevel(), enchantment.value().getMaxLevel());
    }

    public static Holder<Enchantment> getRandomAllowedEnchantment(RegistryAccess registryAccess, RandomSource random, Predicate<Holder<Enchantment>> extraFilter) {
        if (registryAccess == null) return null;
        //? if <1.19.3 {
        /*var lookupOpt = HolderLookup.Provider.of(registryAccess).lookup(Registries.ENCHANTMENT);
        *///?} else {
        var lookupOpt = registryAccess.lookup(Registries.ENCHANTMENT);
        //?}
        if (lookupOpt.isEmpty()) return null;

        List<Holder<Enchantment>> allowed = lookupOpt.get().listElements()
                .map(h -> (Holder<Enchantment>) h)
                .filter(h -> !RuleManager.isEnchantmentBlocked(h))
                .toList();
        List<Holder<Enchantment>> candidates = extraFilter == null ? allowed : allowed.stream().filter(extraFilter).toList();
        if (candidates.isEmpty()) candidates = allowed;
        if (candidates.isEmpty()) return null;
        return candidates.get(random.nextInt(candidates.size()));
    }
}
