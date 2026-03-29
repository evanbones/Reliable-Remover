package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
        if (RuleManager.isInteractionBlocked((ItemStack) (Object) this, context.getLevel(), null)) {
            Player player = context.getPlayer();
            if (player != null && ModConfig.get().showRemovalMessage) {
                player.sendSystemMessage(Component.translatable("message.reliable_remover.interaction_disabled"));
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$entityInteraction(Player player, LivingEntity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (RuleManager.isInteractionBlocked((ItemStack) (Object) this, player.level(), entity)) {
            if (ModConfig.get().showRemovalMessage) {
                player.sendSystemMessage(Component.translatable("message.reliable_remover.interaction_disabled"));
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void reliable_remover$addTooltip(Item.TooltipContext tooltipContext, Player player, TooltipFlag tooltipFlag, CallbackInfoReturnable<List<Component>> cir) {
        if (RuleManager.isHidden((ItemStack) (Object) this, player != null ? player.level() : null)) {
            cir.getReturnValue().add(Component.translatable("tooltip.reliable_remover.item_disabled"));
        }
    }
}