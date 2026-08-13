package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

    @Shadow
    protected abstract BlockState asState();

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$onUseItemOn(ItemStack itemStack, Level level, Player player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (RuleManager.isBlockInteractionBlocked(asState(), level, hitResult.getBlockPos(), player)) {
            if (player != null && itemStack.isEmpty() && ModConfig.get().showRemovalMessage) {
                player.sendSystemMessage(Component.translatable("message.reliable_remover.interaction_disabled"));
            }
            cir.setReturnValue(InteractionResult.TRY_WITH_EMPTY_HAND);
        }
    }

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$onUseWithoutItem(Level level, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (RuleManager.isBlockInteractionBlocked(asState(), level, hitResult.getBlockPos(), player)) {
            if (player != null && ModConfig.get().showRemovalMessage) {
                player.sendSystemMessage(Component.translatable("message.reliable_remover.interaction_disabled"));
            }
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
