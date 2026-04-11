package com.evandev.reliable_remover.mixin.minecraft;

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
    private Collection<ItemStack> displayItems = ItemStackLinkedSet.createTypeAndComponentsSet();

    @Shadow
    private Set<ItemStack> displayItemsSearchTab = ItemStackLinkedSet.createTypeAndComponentsSet();

    @Inject(method = "buildContents", at = @At(value = "RETURN"))
    private void reliable_remover$filterCreativeTabs(CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo ci) {
        if (!Services.PLATFORM.isPhysicalClient()) return;

        if (!ModConfig.get().removeItemsFromCreativeTabs) return;

        if (this.displayItems != null) {
            this.displayItems.removeIf(RuleManager::isCreativeBlocked);
        }
        if (this.displayItemsSearchTab != null) {
            this.displayItemsSearchTab.removeIf(RuleManager::isCreativeBlocked);
        }
    }
}