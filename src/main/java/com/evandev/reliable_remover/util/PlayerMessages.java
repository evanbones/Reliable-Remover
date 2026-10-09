package com.evandev.reliable_remover.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public class PlayerMessages {
    public static void sendSuccess(CommandSourceStack source, Supplier<Component> message, boolean broadcastToAdmins) {
        //? if <1.20 {
        /*source.sendSuccess(message.get(), broadcastToAdmins);
        *///?} else {
        source.sendSuccess(message, broadcastToAdmins);
        //?}
    }

    public static void actionBar(Player player, Component message) {
        //? if >=26.1 {
        player.sendOverlayMessage(message);
        //?} else {
        /*player.displayClientMessage(message, true);
        *///?}
    }
}
