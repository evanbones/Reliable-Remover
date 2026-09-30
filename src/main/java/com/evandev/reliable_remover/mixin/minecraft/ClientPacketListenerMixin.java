package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.RegistryAccess;
//? if >=1.21 {
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
//?} else {
/*import net.minecraft.network.protocol.game.ClientboundUpdateTagsPacket;
*///?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPacketListener.class, priority = 1100)
public abstract class ClientPacketListenerMixin {

    //? if >=1.21 {
    @Final
    @Shadow
    private RegistryAccess.Frozen registryAccess;
    //?} else {
    /*@Shadow
    public abstract RegistryAccess registryAccess();
    *///?}

    @Inject(method = "handleUpdateTags", at = @At("RETURN"))
    private void reliableRemover$onTagsUpdated(ClientboundUpdateTagsPacket packet, CallbackInfo ci) {
        //? if >=1.21 {
        RuleManager.expandTagRules(this.registryAccess);
        //?} else {
        /*RuleManager.expandTagRules(this.registryAccess());
        *///?}
    }
}
