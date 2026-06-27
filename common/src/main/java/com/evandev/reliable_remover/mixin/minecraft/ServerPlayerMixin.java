package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import com.evandev.reliable_remover.data.Action;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {

    public ServerPlayerMixin(Level level, BlockPos pos, float yRot, GameProfile gameProfile) {
        super(level, pos, yRot, gameProfile);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void reliable_remover$tick(CallbackInfo ci) {
        if (this.tickCount % 20 == 0) {
            for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
                ItemStack stack = this.getInventory().getItem(i);
                if (!stack.isEmpty()) {
                    RuleManager.stripBlockedEnchantments(stack);

                    ItemStack replacement = RuleManager.getReplacement(stack, Action.REMOVE_INVENTORY, this.level(), this, "inventory");
                    if (replacement != null) {
                        this.getInventory().setItem(i, replacement);
                    } else if (RuleManager.isInventoryBlocked(stack, this.level(), this)) {
                        this.getInventory().setItem(i, ItemStack.EMPTY);
                        if (ModConfig.get().showRemovalMessage) {
                            this.displayClientMessage(Component.translatable("message.reliable_remover.item_removed"), true);
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$attack(Entity targetEntity, CallbackInfo ci) {
        if (RuleManager.isAttackBlocked(this.getMainHandItem(), this.level(), targetEntity)) {
            if (ModConfig.get().showAttackMessage) {
                this.displayClientMessage(Component.translatable("message.reliable_remover.attack_disabled"), true);
            }
            ci.cancel();
        }
    }

    @Inject(method = "swing", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$cancelSwing(InteractionHand hand, CallbackInfo ci) {
        if (RuleManager.isHandSwingBlocked(this.getMainHandItem(), this.level())) {
            if (ModConfig.get().showHandSwingMessage) {
                this.displayClientMessage(Component.translatable("message.reliable_remover.swing_disabled"), true);
            }
            ci.cancel();
        }
    }

    @Inject(method = "initMenu", at = @At("HEAD"))
    private void reliable_remover$onInitMenu(AbstractContainerMenu menu, CallbackInfo ci) {
        menu.addSlotListener(new net.minecraft.world.inventory.ContainerListener() {
            @Override
            public void slotChanged(@NotNull AbstractContainerMenu menu, int slotId, @NotNull ItemStack stack) {
                if (!stack.isEmpty() && slotId >= 0 && slotId < menu.slots.size()) {
                    ServerPlayer player = (ServerPlayer) (Object) ServerPlayerMixin.this;
                    ItemStack replacement = RuleManager.getReplacement(stack, Action.REMOVE_INVENTORY, player.level(), player, "inventory");

                    if (replacement != null) {
                        menu.getSlot(slotId).set(replacement);
                    } else if (RuleManager.isInventoryBlocked(stack, player.level(), player)) {
                        menu.getSlot(slotId).set(ItemStack.EMPTY);
                    }
                }
            }

            @Override
            public void dataChanged(@NotNull AbstractContainerMenu menu, int id, int value) {
            }
        });

        if (!ModConfig.get().removeItemsOnInventoryOpen) return;

        boolean changed = false;
        for (Slot slot : menu.slots) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                ItemStack replacement = RuleManager.getReplacement(stack, Action.REMOVE_INVENTORY, this.level(), this, "inventory");
                if (replacement != null) {
                    slot.set(replacement);
                    changed = true;
                } else if (RuleManager.isInventoryBlocked(stack, this.level(), this)) {
                    slot.set(ItemStack.EMPTY);
                    changed = true;
                }
            }
        }

        if (changed && ModConfig.get().showRemovalMessage) {
            this.displayClientMessage(Component.translatable("message.reliable_remover.item_removed"), true);
        }
    }
}