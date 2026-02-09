package com.evandev.reliable_remover.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class ReliableRemoverMixinPlugin implements IMixinConfigPlugin {
    private boolean isEmiLoaded;
    private boolean isEmiLootLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        isEmiLoaded = checkClass("dev.emi.emi.api.recipe.EmiRecipe");
        isEmiLootLoaded = checkClass("fzzyhmstrs.emi_loot.EMILoot");
    }

    private boolean checkClass(String className) {
        try {
            Class.forName(className, false, this.getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(".emi.EmiStackListMixin")) {
            return isEmiLoaded;
        }

        if (mixinClassName.contains(".emi.LootTableParserMixin")) {
            return isEmiLootLoaded;
        }

        return true;
    }

    // Boilerplate methods
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