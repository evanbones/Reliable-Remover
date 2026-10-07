package com.evandev.reliable_remover.client;

import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public class ServerCommands {

    /**
     * Whether the server sent this player {@code /rremover remove}. This means the server has Reliable Remover and the player has permission to use it.
     */
    public static boolean canRemoveItems() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return false;

        CommandNode<?> root = connection.getCommands().getRoot().getChild("rremover");
        return root != null && root.getChild("remove") != null;
    }
}
