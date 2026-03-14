package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.GiveCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(GiveCommand.class)
public class GiveCommandMixin {

    @Inject(
            method = "giveItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/item/ItemInput;createItemStack(IZ)Lnet/minecraft/world/item/ItemStack;"),
            cancellable = true
    )
    private static void reliable_remover$onGiveItem(
            CommandSourceStack source,
            ItemInput item,
            Collection<ServerPlayer> targets,
            int count,
            CallbackInfoReturnable<Integer> cir
    ) throws CommandSyntaxException {
        ItemStack checkStack = item.createItemStack(1, false);

        if (RuleManager.isHidden(checkStack)) {
            if (ModConfig.get().showRemovalMessage) {
                source.sendFailure(Component.translatable("commands.reliable_remover.give.failed", checkStack.getHoverName()));
            }
            cir.setReturnValue(0);
        }
    }
}