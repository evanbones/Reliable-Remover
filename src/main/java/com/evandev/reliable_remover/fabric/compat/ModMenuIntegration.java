package com.evandev.reliable_remover.fabric.compat;

//? if fabric {
import com.evandev.reliable_remover.config.YaclConfigIntegration;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")) {
            return parent -> null;
        }
        return parent -> YaclConfigIntegration.createScreen(parent);
    }
}
//?}
