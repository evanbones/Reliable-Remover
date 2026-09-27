package com.evandev.reliable_remover.mixin.minecraft;

//? if <26.1 {
/*import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.util.PlayerMessages;
import com.evandev.reliable_remover.config.RuleManager;
import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@IfMinecraftVersion(maxVersion = "1.21.1", maxInclusive = true)
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
    private void reliable_remover$cancelInteractAt(Player player, Vec3 vec, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        Entity entity = (Entity) (Object) this;
        if (RuleManager.isInteractionBlocked(stack, player.level(), entity)) {
            if (ModConfig.get().showRemovalMessage) {
                PlayerMessages.actionBar(player, Component.translatable("message.reliable_remover.interaction_disabled"));
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
*///?} else {
import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@IfMinecraftVersion(maxVersion = "1.21.1")
@Mixin(Entity.class)
public abstract class EntityMixin {
}
//?}
