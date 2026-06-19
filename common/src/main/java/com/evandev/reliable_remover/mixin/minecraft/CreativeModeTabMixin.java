package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import com.evandev.reliable_remover.platform.Services;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
import java.util.Set;

@Mixin(value = CreativeModeTab.class, priority = 10000)
public abstract class CreativeModeTabMixin {

    @Shadow
    private Collection<ItemStack> displayItems = ItemStackLinkedSet.createTypeAndComponentsSet();

    @Shadow
    private Set<ItemStack> displayItemsSearchTab = ItemStackLinkedSet.createTypeAndComponentsSet();

    @WrapMethod(method = "buildContents")
    private void reliable_remover$filterCreativeTabs(CreativeModeTab.ItemDisplayParameters displayContext, Operation<Void> original) {
        original.call(displayContext);

        if (!Services.PLATFORM.isPhysicalClient()) return;

        if (!ModConfig.get().removeItemsFromCreativeTabs) return;

        if (this.displayItems != null) {
            this.displayItems.removeIf(RuleManager::isCreativeBlockedIgnoringAdvancements);
        }
        if (this.displayItemsSearchTab != null) {
            this.displayItemsSearchTab.removeIf(RuleManager::isCreativeBlockedIgnoringAdvancements);
        }
    }
}