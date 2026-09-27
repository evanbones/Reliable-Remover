package com.evandev.reliable_remover.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class PlayerMessages {
    public static void actionBar(Player player, Component message) {
        //? if >=26.1 {
        player.sendOverlayMessage(message);
        //?} else {
        /*player.displayClientMessage(message, true);
        *///?}
    }
}
