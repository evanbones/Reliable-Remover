package com.evandev.reliable_remover.compat;

//? if <=1.21.1 {
/*import com.evandev.reliable_remover.config.ModConfig;
import com.evandev.reliable_remover.config.RuleManager;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

public class EmiBlacklistHelper {

    public static boolean isEmiStackBlacklisted(EmiStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        Identifier id = stack.getId();
        if (id != null) {
            if (ModConfig.get().blacklistedItems.contains(id.toString())) {
                return true;
            }

            if (id.getNamespace().equals("jeed")) {
                for (String blacklisted : ModConfig.get().blacklistedItems) {
                    Identifier blId = Identifier.tryParse(blacklisted);
                    if (blId != null && id.getPath().contains(blId.getPath())) {
                        return true;
                    }
                }
                for (var entry : BuiltInRegistries.MOB_EFFECT.entrySet()) {
                    Identifier effectId = entry.getKey().identifier();
                    if (id.getPath().contains(effectId.getPath()) || id.toString().contains(effectId.getPath())) {
                        Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(entry.getValue());
                        if (RuleManager.isEffectCreativeBlocked(holder)) {
                            return true;
                        }
                    }
                }
            }
        }

        Object key = stack.getKey();
        if (key instanceof Holder<?> holder && holder.value() instanceof MobEffect effect) {
            Holder<MobEffect> mobEffectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
            return RuleManager.isEffectCreativeBlocked(mobEffectHolder);
        } else if (key instanceof MobEffect effect) {
            Holder<MobEffect> mobEffectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
            return RuleManager.isEffectCreativeBlocked(mobEffectHolder);
        }

        ItemStack itemStack = stack.getItemStack();
        if ((itemStack == null || itemStack.isEmpty()) && id != null) {
            return RuleManager.isFluidHidden(id.toString());
        }

        return false;
    }
}
*///?}
