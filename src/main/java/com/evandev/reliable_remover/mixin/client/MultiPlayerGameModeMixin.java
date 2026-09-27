package com.evandev.reliable_remover.mixin.client;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import com.evandev.reliable_remover.util.PlayerMessages;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$cancelUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult result, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        Level level = player.level();

        if (RuleManager.isPlacementBlocked(stack, level, player)) {
            reliable_remover$showMessage(player, "message.reliable_remover.placement_disabled");
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        BlockPos pos = result.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!(stack.getItem() instanceof BlockItem) && RuleManager.isBlockInteractionBlocked(state, level, pos, player)) {
            reliable_remover$showMessage(player, "message.reliable_remover.interaction_disabled");
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$cancelUseItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (RuleManager.isInteractionBlocked(stack, player.level(), null)) {
            reliable_remover$showMessage(player, "message.reliable_remover.interaction_disabled");
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Unique
    private static void reliable_remover$showMessage(Player player, String key) {
        if (!ModConfig.get().showRemovalMessage) return;
        PlayerMessages.actionBar(player, Component.translatable(key));
    }
}
