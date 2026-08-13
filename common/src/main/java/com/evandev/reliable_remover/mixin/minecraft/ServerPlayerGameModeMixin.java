package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$cancelUseItem(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (RuleManager.isInteractionBlocked(stack, level, null)) {
            if (ModConfig.get().showRemovalMessage) {
                player.displayClientMessage(Component.translatable("message.reliable_remover.interaction_disabled"), true);
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$cancelUseItemOn(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (RuleManager.isPlacementBlocked(stack, level, player)) {
            if (ModConfig.get().showRemovalMessage) {
                player.displayClientMessage(Component.translatable("message.reliable_remover.placement_disabled"), true);
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}