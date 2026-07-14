package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundUpdateTagsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPacketListener.class, priority = 1100)
public class ClientPacketListenerMixin {
    @Inject(method = "handleUpdateTags", at = @At("RETURN"))
    private void reliableRemover$onTagsUpdated(ClientboundUpdateTagsPacket packet, CallbackInfo ci) {
        RuleManager.expandTagRules();
    }
}
