package com.evandev.reliable_remover.util.legacy;

//? if <1.19.3 {
/*import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public final class Registries {
    public static final ResourceKey<Registry<Item>> ITEM = Registry.ITEM_REGISTRY;
    public static final ResourceKey<Registry<Block>> BLOCK = Registry.BLOCK_REGISTRY;
    public static final ResourceKey<Registry<Fluid>> FLUID = Registry.FLUID_REGISTRY;
    public static final ResourceKey<Registry<EntityType<?>>> ENTITY_TYPE = Registry.ENTITY_TYPE_REGISTRY;
    public static final ResourceKey<Registry<Potion>> POTION = Registry.POTION_REGISTRY;
    public static final ResourceKey<Registry<MobEffect>> MOB_EFFECT = Registry.MOB_EFFECT_REGISTRY;
    public static final ResourceKey<Registry<Enchantment>> ENCHANTMENT = Registry.ENCHANTMENT_REGISTRY;

    private Registries() {
    }
}
*///?}
