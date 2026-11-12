package com.evandev.reliable_remover.client;

import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public class ServerCommands {

    public static boolean canRemoveItems() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return false;

        CommandNode<?> root = connection.getCommands().getRoot().getChild("rremover");
        return root != null && root.getChild("remove") != null;
    }
}
