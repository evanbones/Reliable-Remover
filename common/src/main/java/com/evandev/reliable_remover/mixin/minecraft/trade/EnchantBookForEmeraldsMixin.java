package com.evandev.reliable_remover.mixin.minecraft.trade;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$EnchantBookForEmeralds")
public class EnchantBookForEmeraldsMixin {

    @Inject(method = "getOffer", at = @At("RETURN"), cancellable = true)
    private void reliable_remover$rerollOrCleanBookOffer(Entity trader, RandomSource random, CallbackInfoReturnable<MerchantOffer> cir) {
        MerchantOffer offer = cir.getReturnValue();
        if (offer == null) return;

        ItemStack result = offer.getResult();
        if (result.is(Items.ENCHANTED_BOOK)) {
            RuleManager.stripBlockedEnchantments(result);

            if (EnchantedBookItem.getEnchantments(result).isEmpty()) {
                ItemStack baseCost = offer.getBaseCostA();
                MerchantOffer replacementOffer = new MerchantOffer(
                        baseCost,
                        new ItemStack(Items.BOOK),
                        offer.getMaxUses(),
                        offer.getXp(),
                        offer.getPriceMultiplier()
                );
                cir.setReturnValue(replacementOffer);
            }
        }
    }
}
