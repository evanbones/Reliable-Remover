package com.evandev.reliable_remover.mixin.minecraft.trade;

//? if <=1.21.1 {
/*import com.evandev.reliable_remover.config.RuleManager;
import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@IfMinecraftVersion(maxVersion = "1.21.1", maxInclusive = true)
@Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$EnchantedItemForEmeralds")
public class EnchantedItemForEmeraldsMixin {

    @Inject(method = "getOffer", at = @At("RETURN"))
    private void reliable_remover$stripTradeItemEnchantments(Entity trader, RandomSource random, CallbackInfoReturnable<MerchantOffer> cir) {
        MerchantOffer offer = cir.getReturnValue();
        if (offer != null && !offer.getResult().isEmpty()) {
            RuleManager.stripBlockedEnchantments(offer.getResult(), trader.level());
        }
    }
}
*///?} else {
import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import org.spongepowered.asm.mixin.Mixin;

@IfMinecraftVersion(maxVersion = "1.21.1", maxInclusive = true)
@Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$EnchantedItemForEmeralds")
public class EnchantedItemForEmeraldsMixin {
}
//?}
