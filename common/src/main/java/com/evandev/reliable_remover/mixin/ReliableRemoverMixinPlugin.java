package com.evandev.reliable_remover.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class ReliableRemoverMixinPlugin implements IMixinConfigPlugin {
    private boolean isEmiLoaded;
    private boolean isEmiLootLoaded;
    private boolean isCnmLoaded;
    private boolean isJeedLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        isEmiLoaded = checkClass("dev.emi.emi.api.recipe.EmiRecipe");
        isEmiLootLoaded = checkClass("fzzyhmstrs.emi_loot.EMILoot");
        isCnmLoaded = checkClass("dev.tazer.clutternomore.common.shape_map.ShapeMap");
        isJeedLoaded = checkClass("net.mehvahdjukaar.jeed.Jeed");
    }

    private boolean checkClass(String className) {
        String path = className.replace('.', '/') + ".class";
        return this.getClass().getClassLoader().getResource(path) != null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(".emi.")) {
            if (mixinClassName.contains("LootTableParserMixin")) {
                return isEmiLootLoaded;
            }
            return isEmiLoaded;
        }

        if (mixinClassName.contains(".clutternomore.")) {
            return isCnmLoaded;
        }

        if (mixinClassName.contains(".jeed.")) {
            return isJeedLoaded;
        }

        return true;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}