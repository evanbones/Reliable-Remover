package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Set;

@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabMixin {

    @Shadow private Collection<ItemStack> displayItems;
    @Shadow private Set<ItemStack> displayItemsSearchTab;

    @Inject(method = "buildContents", at = @At("RETURN"))
    private void reliable_remover$filterCreativeTabs(CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo ci) {
        if (this.displayItems != null) {
            this.displayItems.removeIf(RuleManager::isHidden);
        }
        if (this.displayItemsSearchTab != null) {
            this.displayItemsSearchTab.removeIf(RuleManager::isHidden);
        }
    }
}