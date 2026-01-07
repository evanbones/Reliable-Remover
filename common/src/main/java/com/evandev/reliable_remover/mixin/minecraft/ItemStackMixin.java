package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$blockUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (RuleManager.isInteractionBlocked((ItemStack) (Object) this, context.getLevel())) {
            Player player = context.getPlayer();
            if (player != null && ModConfig.get().showRemovalMessage) {
                player.displayClientMessage(Component.translatable("message.reliable_remover.interaction_disabled"), true);
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void reliable_remover$addTooltip(Player player, TooltipFlag isAdvanced, CallbackInfoReturnable<List<Component>> cir) {
        if (RuleManager.isHidden((ItemStack) (Object) this, player != null ? player.level() : null)) {
            cir.getReturnValue().add(Component.literal("§4Unobtainable"));
        }
    }
}