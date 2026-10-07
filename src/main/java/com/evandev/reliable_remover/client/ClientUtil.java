package com.evandev.reliable_remover.client;

//? if >=26.3 {
/*import com.mojang.blaze3d.Blaze3D;
*///?} else if >=26.1 {
import net.minecraft.util.Util;
//?} else {
/*import net.minecraft.Util;
*///?}

import java.nio.file.Path;

public final class ClientUtil {
    private ClientUtil() {
    }

    public static void openFolder(Path folder) {
        //? if >=26.3 {
        /*Blaze3D.openPath(folder);
        *///?} else {
        Util.getPlatform().openUri(folder.toUri());
        //?}
    }
}
