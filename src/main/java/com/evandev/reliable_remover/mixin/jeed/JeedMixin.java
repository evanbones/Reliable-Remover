package com.evandev.reliable_remover.mixin.jeed;

//? if <26.2 {
/*import com.evandev.reliable_remover.config.RuleManager;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import net.mehvahdjukaar.jeed.Jeed;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@IfModLoaded("jeed")
@Mixin(value = Jeed.class, remap = false)
public class JeedMixin {

    /^*
     * Intercepts JEED's effect list generation and filters out any
     * effects whose IDs are present in Reliable Remover's blacklist or rules.
     ^/
    @Inject(method = "getEffectList", at = @At("RETURN"), cancellable = true)
    private static void reliable_remover$filterJeedEffects(CallbackInfoReturnable<List<Holder.Reference<MobEffect>>> cir) {
        List<Holder.Reference<MobEffect>> originalList = cir.getReturnValue();
        if (originalList != null && !originalList.isEmpty()) {
            List<Holder.Reference<MobEffect>> filteredList = new ArrayList<>(originalList);
            var player = Minecraft.getInstance().player;
            filteredList.removeIf(holder -> RuleManager.isEffectCreativeBlocked(holder, player));

            if (filteredList.size() != originalList.size()) {
                cir.setReturnValue(filteredList);
            }
        }
    }
}
*///?} else {
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import org.spongepowered.asm.mixin.Mixin;

@IfModLoaded("jeed")
@Mixin(targets = "net.mehvahdjukaar.jeed.Jeed", remap = false)
public class JeedMixin {
}
//?}
