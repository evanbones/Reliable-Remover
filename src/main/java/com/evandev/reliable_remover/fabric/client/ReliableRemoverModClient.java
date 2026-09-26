package com.evandev.reliable_remover.fabric.client;

//? if fabric {
import com.evandev.reliable_remover.client.Keybinds;
import net.fabricmc.api.ClientModInitializer;

//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

public class ReliableRemoverModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(Keybinds.RRV_DELETE_KEY);
    }
}
//?} else {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

public class ReliableRemoverModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(Keybinds.EMI_DELETE_KEY);
    }
}
*///?}
//?}
