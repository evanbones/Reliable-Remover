package com.evandev.reliable_remover.util.legacy;

//? if <1.19.3 {
/*import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public final class BuiltInRegistries {
    public static final DefaultedRegistry<Item> ITEM = Registry.ITEM;
    public static final DefaultedRegistry<Block> BLOCK = Registry.BLOCK;
    public static final DefaultedRegistry<Fluid> FLUID = Registry.FLUID;
    public static final DefaultedRegistry<EntityType<?>> ENTITY_TYPE = Registry.ENTITY_TYPE;
    public static final DefaultedRegistry<Potion> POTION = Registry.POTION;
    public static final Registry<MobEffect> MOB_EFFECT = Registry.MOB_EFFECT;
    public static final Registry<Enchantment> ENCHANTMENT = Registry.ENCHANTMENT;

    private BuiltInRegistries() {
    }

    public static <T> Holder<T> wrapAsHolder(Registry<T> registry, T value) {
        return registry.getResourceKey(value).flatMap(registry::getHolder).orElseGet(() -> Holder.direct(value));
    }
}
*///?}
