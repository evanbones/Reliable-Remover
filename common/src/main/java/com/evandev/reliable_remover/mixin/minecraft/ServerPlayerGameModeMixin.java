package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {

    @Final
    @Shadow protected ServerPlayer player;

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$cancelUseItem(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (RuleManager.isInteractionBlocked(stack)) {
            player.displayClientMessage(Component.literal("Item interactions are disabled."), true);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}