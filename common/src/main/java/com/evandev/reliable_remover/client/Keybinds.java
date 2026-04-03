package com.evandev.reliable_remover.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    public static final KeyMapping EMI_DELETE_KEY = new KeyMapping(
            "key.reliable_remover.emi_delete",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_DELETE,
            "category.reliable_remover"
    );
}