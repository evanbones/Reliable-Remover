package com.evandev.reliable_remover.mixin.minecraft;

//? if <1.19.3 {
/*import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import com.evandev.reliable_remover.platform.Services;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CreativeModeTab.class, priority = 10000)
public abstract class CreativeModeTabMixin {

    @Inject(method = "fillItemList", at = @At("RETURN"))
    private void reliable_remover$filterCreativeTab(NonNullList<ItemStack> items, CallbackInfo ci) {
        if (!Services.PLATFORM.isPhysicalClient()) return;
        if (!ModConfig.get().removeItemsFromCreativeTabs) return;

        items.removeIf(RuleManager::isCreativeBlocked);
    }
}
*///?} else {
import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import com.evandev.reliable_remover.platform.Services;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Set;

@Mixin(value = CreativeModeTab.class, priority = 10000)
public abstract class CreativeModeTabMixin {

    @Shadow
    private Collection<ItemStack> displayItems;

    @Shadow
    private Set<ItemStack> displayItemsSearchTab;

    //? if >=1.21 {
    @Inject(method = "buildContents", at = @At(value = "RETURN"))
    //?} else {
    /*@Inject(method = "buildContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTab;rebuildSearchTree()V"))
    *///?}
    private void reliable_remover$filterCreativeTabs(CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo ci) {
        if (!Services.PLATFORM.isPhysicalClient()) return;

        if (!ModConfig.get().removeItemsFromCreativeTabs) return;

        // Rebuild safely instead of relying on modifying potentially unmodifiable collections
        if (this.displayItems != null) {
            //? if >=1.21 {
            Collection<ItemStack> filtered = ItemStackLinkedSet.createTypeAndComponentsSet();
            //?} else {
            /*Collection<ItemStack> filtered = ItemStackLinkedSet.createTypeAndTagSet();
            *///?}
            for (ItemStack stack : this.displayItems) {
                if (!RuleManager.isCreativeBlocked(stack)) {
                    filtered.add(stack);
                }
            }
            this.displayItems = filtered;
        }

        if (this.displayItemsSearchTab != null) {
            //? if >=1.21 {
            Set<ItemStack> filteredSearch = ItemStackLinkedSet.createTypeAndComponentsSet();
            //?} else {
            /*Set<ItemStack> filteredSearch = ItemStackLinkedSet.createTypeAndTagSet();
            *///?}
            for (ItemStack stack : this.displayItemsSearchTab) {
                if (!RuleManager.isCreativeBlocked(stack)) {
                    filteredSearch.add(stack);
                }
            }
            this.displayItemsSearchTab = filteredSearch;
        }
    }
}
//?}
