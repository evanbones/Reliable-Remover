package com.evandev.reliable_remover.neoforge;

//? if neoforge {
/*import com.evandev.reliable_remover.CommonClass;
import com.evandev.reliable_remover.Constants;
import com.evandev.reliable_remover.command.ReliableRemoverCommands;
import com.evandev.reliable_remover.config.ReloadListener;
import com.evandev.reliable_remover.neoforge.client.ClientSetup;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
//? if >=26.1 {
/^import net.neoforged.neoforge.event.AddServerReloadListenersEvent;^/
//?} else {
import net.neoforged.neoforge.event.AddReloadListenerEvent;
//?}
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(Constants.MOD_ID)
public class ReliableRemoverMod {

    public ReliableRemoverMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.addListener(this::addReloadListener);
        NeoForge.EVENT_BUS.addListener(ReliableRemoverMod::onRegisterCommands);

        //? if >=26.1 {
        /^if (FMLEnvironment.getDist().isClient()) {^/
        //?} else {
        if (FMLEnvironment.dist.isClient()) {
        //?}
            ClientSetup.init(modEventBus, modContainer);
        }
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ReliableRemoverCommands.register(event.getDispatcher());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        CommonClass.init();
    }

    //? if >=26.1 {
    /^private void addReloadListener(final AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "rule_reloader"), new ReloadListener());
    }^/
    //?} else {
    private void addReloadListener(final AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }
    //?}
}
*///?}
