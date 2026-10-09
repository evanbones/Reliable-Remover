package com.evandev.reliable_remover.mixin.clutternomore;

//? if (forge && >=1.20) || >=1.21 {
import com.evandev.reliable_remover.api.ReliableRemoverAPI;
import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import dev.tazer.clutternomore.common.shape_map.ShapeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@IfModLoaded("clutternomore")
@Mixin(value = ShapeMap.class, remap = false)
public class ShapeMapMixin {
    @Unique
    private static volatile List<ShapeMap.Mapping> reliable_remover$lastMappings = null;

    @Unique
    private static boolean reliable_remover$reapplying = false;

    /**
     * Filter out any mappings where either the parent or the shape itself is hidden by Reliable Remover.
     * When removeCnmChildren is enabled, also cascades removal to child shapes whose parent is removed.
     */
    @Inject(method = "setMappings", at = @At("HEAD"))
    private static void reliable_remover$filterRemovedShapes(List<ShapeMap.Mapping> mappings, boolean detailedLogs, CallbackInfo ci) {
        if (reliable_remover$reapplying) return;

        reliable_remover$lastMappings = new ArrayList<>(mappings);
        RuleManager.registerCnmCascadeRecompute(ShapeMapMixin::reliable_remover$refresh);
        if (mappings.isEmpty() || !reliable_remover$itemsReady()) return;

        reliable_remover$recomputeCascade(mappings);
        mappings.removeIf(ShapeMapMixin::reliable_remover$isRemoved);
    }

    @Unique
    private static void reliable_remover$refresh() {
        List<ShapeMap.Mapping> mappings = reliable_remover$lastMappings;
        if (mappings == null || !reliable_remover$itemsReady()) return;

        reliable_remover$recomputeCascade(mappings);

        List<ShapeMap.Mapping> filtered = new ArrayList<>(mappings);
        filtered.removeIf(ShapeMapMixin::reliable_remover$isRemoved);
        reliable_remover$reapplying = true;
        try {
            ShapeMap.setMappings(filtered, false);
        } finally {
            reliable_remover$reapplying = false;
        }
    }

    @Unique
    private static boolean reliable_remover$isRemoved(ShapeMap.Mapping mapping) {
        return ReliableRemoverAPI.isItemHidden(new ItemStack(mapping.parent())) ||
                ReliableRemoverAPI.isItemHidden(new ItemStack(mapping.shape()));
    }

    @Unique
    private static void reliable_remover$recomputeCascade(List<ShapeMap.Mapping> mappings) {
        if (ModConfig.get().removeCnmChildren) {
            Set<String> cascade = ConcurrentHashMap.newKeySet();
            for (ShapeMap.Mapping mapping : mappings) {
                if (ReliableRemoverAPI.isItemHidden(new ItemStack(mapping.parent()))) {
                    Identifier shapeId = BuiltInRegistries.ITEM.getKey(mapping.shape());
                    cascade.add(shapeId.toString());
                }
            }
            RuleManager.setCnmCascadeRemoved(cascade);
        } else {
            RuleManager.setCnmCascadeRemoved(ConcurrentHashMap.newKeySet());
        }
    }

    @Unique
    private static boolean reliable_remover$itemsReady() {
        //? if >=26.1 {
        return Items.STONE.builtInRegistryHolder().areComponentsBound();
        //?} else {
        /*return true;
        *///?}
    }
}
//?} else {
/*import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@IfModLoaded("clutternomore")
@Pseudo
@Mixin(targets = "dev.tazer.clutternomore.common.shape_map.ShapeMap", remap = false)
public class ShapeMapMixin {
}
*///?}
