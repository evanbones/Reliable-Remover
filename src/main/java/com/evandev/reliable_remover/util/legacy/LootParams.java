package com.evandev.reliable_remover.util.legacy;

//? if <1.20 {
/*import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import org.jetbrains.annotations.Nullable;

public final class LootParams {
    private final LootContext context;

    public LootParams(LootContext context) {
        this.context = context;
    }

    public ServerLevel getLevel() {
        return context.getLevel();
    }

    @Nullable
    public <T> T getParamOrNull(LootContextParam<T> param) {
        return context.getParamOrNull(param);
    }
}
*///?}
