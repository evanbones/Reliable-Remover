package com.evandev.reliable_remover.mixin.minecraft;

import com.evandev.reliable_remover.config.RuleManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
        map.entrySet().removeIf(entry -> {
            try {
                if (!entry.getValue().isJsonObject()) return false;
                JsonObject json = entry.getValue().getAsJsonObject();
                String resultItem = null;

                if (json.has("result")) {
                    JsonElement result = json.get("result");
                    if (result.isJsonObject() && result.getAsJsonObject().has("item")) {
                        resultItem = result.getAsJsonObject().get("item").getAsString();
                    } else if (result.isJsonPrimitive()) {
                        resultItem = result.getAsString();
                    }
                }
                else if (json.has("output")) {
                    resultItem = json.get("output").getAsString();
                }

                if (resultItem != null && RuleManager.isHidden(resultItem)) {
                    return true;
                }
            } catch (Exception ignored) {
                // TODO: log error
            }
            return false;
        });
    }
}