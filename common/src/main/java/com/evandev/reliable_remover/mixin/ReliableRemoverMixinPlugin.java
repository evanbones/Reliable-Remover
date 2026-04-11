package com.evandev.reliable_remover.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class ReliableRemoverMixinPlugin implements IMixinConfigPlugin {
    private boolean isRRVLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        try {
            Class.forName("cc.cassian.rrv.common.ReliableRecipeViewer", false, this.getClass().getClassLoader());
            isRRVLoaded = true;
        } catch (ClassNotFoundException e) {
            isRRVLoaded = false;
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(".rrv.")) {
            return isRRVLoaded;
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