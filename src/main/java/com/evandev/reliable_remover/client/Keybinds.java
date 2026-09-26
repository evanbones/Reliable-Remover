package com.evandev.reliable_remover.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

//? if >=26.1 {
import com.evandev.reliable_remover.Constants;
import net.minecraft.resources.Identifier;

public class Keybinds {
    public static final KeyMapping.Category RELIABLE_REMOVER_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "main"));

    public static final KeyMapping RRV_DELETE_KEY = new KeyMapping(
            "key.reliable_remover.rrv_delete",
            InputConstants.KEY_DELETE,
            RELIABLE_REMOVER_CATEGORY
    );
}
//?} else {
/*import org.lwjgl.glfw.GLFW;

public class Keybinds {
    public static final KeyMapping EMI_DELETE_KEY = new KeyMapping(
            "key.reliable_remover.emi_delete",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_DELETE,
            "category.reliable_remover"
    );
}
*///?}