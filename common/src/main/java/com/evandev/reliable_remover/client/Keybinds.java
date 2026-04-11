package com.evandev.reliable_remover.client;

import com.evandev.reliable_remover.Constants;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    public static final KeyMapping.Category RELIABLE_REMOVER_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "main"));

    public static final KeyMapping RRV_DELETE_KEY = new KeyMapping(
            "key.reliable_remover.rrv_delete",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_DELETE,
            RELIABLE_REMOVER_CATEGORY
    );
}