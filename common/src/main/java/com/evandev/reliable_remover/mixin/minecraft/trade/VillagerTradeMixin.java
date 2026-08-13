package com.evandev.reliable_remover.mixin.minecraft.trade;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(VillagerTrade.class)
public class VillagerTradeMixin {

    @Inject(method = "getOffer", at = @At("RETURN"), cancellable = true)
    private void reliable_remover$stripTradeEnchantments(LootContext lootContext, CallbackInfoReturnable<MerchantOffer> cir) {
        MerchantOffer offer = cir.getReturnValue();
        if (offer == null) return;

        ItemStack result = offer.getResult();
        if (!result.isEmpty()) {
            RuleManager.stripBlockedEnchantments(result, lootContext != null ? lootContext.getLevel() : null);

            if (result.is(Items.ENCHANTED_BOOK)) {
                ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
                if (enchantments == null || enchantments.isEmpty()) {
                    ItemCost emeraldCost = offer.getItemCostA();
                    MerchantOffer replacementOffer = new MerchantOffer(
                            emeraldCost,
                            Optional.of(new ItemCost(Items.BOOK)),
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
}
