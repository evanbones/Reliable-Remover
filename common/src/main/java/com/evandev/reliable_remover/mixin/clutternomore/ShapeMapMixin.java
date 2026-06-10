package com.evandev.reliable_remover.mixin.clutternomore;

import com.evandev.reliable_remover.api.ReliableRemoverAPI;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ShapeMap.class)
public class ShapeMapMixin {

    /**
     * Filter out any mappings where either the parent or the shape itself is hidden by Reliable Remover.
     */
    @Inject(method = "setMappings", at = @At("HEAD"), remap = false)
    private static void reliable_remover$filterRemovedShapes(List<ShapeMap.Mapping> mappings, boolean detailedLogs, CallbackInfo ci) {
        mappings.removeIf(mapping ->
                ReliableRemoverAPI.isItemHidden(new ItemStack(mapping.parent())) ||
                        ReliableRemoverAPI.isItemHidden(new ItemStack(mapping.shape()))
        );
    }
}