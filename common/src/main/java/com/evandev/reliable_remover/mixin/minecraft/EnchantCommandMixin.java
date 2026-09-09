package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.EnchantCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(EnchantCommand.class)
public class EnchantCommandMixin {

    @Unique
    private static final DynamicCommandExceptionType ERROR_INCOMPATIBLE = new DynamicCommandExceptionType(
            item -> Component.translatableEscape("commands.enchant.failed.incompatible", item)
    );

    @Inject(method = "enchant", at = @At("HEAD"))
    private static void reliable_remover$checkEnchantmentBlocked(
            CommandSourceStack source,
            Collection<? extends Entity> targets,
            Holder<Enchantment> enchantment,
            int level,
            CallbackInfoReturnable<Integer> cir
    ) throws CommandSyntaxException {
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity livingEntity) {
                ItemStack stack = livingEntity.getMainHandItem();
                if (!stack.isEmpty() && RuleManager.isEnchantmentBlocked(stack, enchantment)) {
                    if (targets.size() == 1) {
                        throw ERROR_INCOMPATIBLE.create(stack.getItem().getName(stack).getString());
                    }
                }
            }
        }
    }
}
