package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$cancelInteractOn(Entity entityToInteractOn, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = (Player) (Object) this;
        ItemStack stack = player.getItemInHand(hand);
        if (RuleManager.isInteractionBlocked(stack, player.level(), entityToInteractOn)) {
            if (ModConfig.get().showRemovalMessage) {
                player.displayClientMessage(Component.translatable("message.reliable_remover.interaction_disabled"), true);
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
