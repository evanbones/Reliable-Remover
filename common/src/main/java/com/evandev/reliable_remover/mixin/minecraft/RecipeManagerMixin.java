package com.evandev.reliable_remover.mixin.minecraft;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    @Inject(method = "apply", at = @At("HEAD"))
    protected void reliable_remover$interceptRecipes(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        // Iterate map and remove entries where result matches RuleManager.isHidden()
        map.entrySet().removeIf(entry -> {
            // ... parse result item ID from entry.getValue() ...
            // return RuleManager.isHidden(resultId);
            return false; // placeholder
        });
    }
}