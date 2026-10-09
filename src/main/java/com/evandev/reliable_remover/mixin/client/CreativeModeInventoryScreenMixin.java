package com.evandev.reliable_remover.mixin.client;

//? if <1.19.3 {
/*import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void reliable_remover$filterTab(CreativeModeTab tab, CallbackInfo ci) {
        if (tab != CreativeModeTab.TAB_HOTBAR) {
            this.reliable_remover$filterDisplayedItems();
        }
    }

    @Inject(method = "refreshSearchResults", at = @At("TAIL"))
    private void reliable_remover$filterSearch(CallbackInfo ci) {
        this.reliable_remover$filterDisplayedItems();
    }

    @Unique
    private void reliable_remover$filterDisplayedItems() {
        if (!ModConfig.get().removeItemsFromCreativeTabs) return;

        ((CreativeModeInventoryScreen) (Object) this).getMenu().items.removeIf(RuleManager::isCreativeBlocked);
    }
}
*///?} else {
import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;

@IfMinecraftVersion(maxVersion = "1.19.2")
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
}
//?}
