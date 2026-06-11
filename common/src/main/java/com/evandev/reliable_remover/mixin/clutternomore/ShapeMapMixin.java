package com.evandev.reliable_remover.mixin.clutternomore;

import com.evandev.reliable_remover.api.ReliableRemoverAPI;
import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ShapeMap.class)
public class ShapeMapMixin {

    /**
     * Filter out any mappings where either the parent or the shape itself is hidden by Reliable Remover.
     * When removeCnmChildren is enabled, also cascades removal to child shapes whose parent is removed.
     */
    @Inject(method = "setMappings", at = @At("HEAD"), remap = false)
    private static void reliable_remover$filterRemovedShapes(List<ShapeMap.Mapping> mappings, boolean detailedLogs, CallbackInfo ci) {
        if (ModConfig.get().removeCnmChildren) {
            Set<String> cascade = ConcurrentHashMap.newKeySet();
            for (ShapeMap.Mapping mapping : mappings) {
                if (ReliableRemoverAPI.isItemHidden(new ItemStack(mapping.parent()))) {
                    ResourceLocation shapeId = BuiltInRegistries.ITEM.getKey(mapping.shape());
                    cascade.add(shapeId.toString());
                }
            }
            RuleManager.setCnmCascadeRemoved(cascade);
        } else {
            RuleManager.setCnmCascadeRemoved(ConcurrentHashMap.newKeySet());
        }

        mappings.removeIf(mapping ->
                ReliableRemoverAPI.isItemHidden(new ItemStack(mapping.parent())) ||
                        ReliableRemoverAPI.isItemHidden(new ItemStack(mapping.shape()))
        );
    }
}