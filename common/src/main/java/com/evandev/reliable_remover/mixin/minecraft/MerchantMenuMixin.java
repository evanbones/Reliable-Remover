package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantMenu.class)
public class MerchantMenuMixin {

    @Inject(method = "getOffers", at = @At("RETURN"))
    private void reliable_remover$filterTrades(CallbackInfoReturnable<MerchantOffers> cir) {
        MerchantOffers offers = cir.getReturnValue();
        if (offers != null) {
            offers.removeIf(offer ->
                    RuleManager.isTradeBlocked(offer.getResult()) ||
                            RuleManager.isTradeBlocked(offer.getBaseCostA()) ||
                            RuleManager.isTradeBlocked(offer.getCostB())
            );
        }
    }
}