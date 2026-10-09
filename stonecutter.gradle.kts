plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.18.2" apply false
    id("net.neoforged.moddev") version "2.0.147" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.147" apply false
    id("dev.kikugie.postprocess.jsonlang") version "2.1-beta.4" apply false
    id("me.modmuss50.mod-publish-plugin") version "2.2.1" apply false
}

stonecutter active "26.2-fabric"

stonecutter parameters {
    constants.match(node.metadata.project.substringAfterLast('-'), "fabric", "forge", "neoforge")
    filters.include("**/*.fsh", "**/*.vsh")
    replacements {
        string(eval(node.metadata.version, ">=26.1")) {
            replace("net.minecraft.resources.ResourceLocation", "net.minecraft.resources.Identifier")
            replace("ResourceLocation", "Identifier")
            replace("net.minecraft.commands.arguments.ResourceLocationArgument", "net.minecraft.commands.arguments.IdentifierArgument")
            replace("ResourceLocationArgument", "IdentifierArgument")
            replace(".location()", ".identifier()")
            replace("instance.enchantment", "instance.enchantment()")
        }

        val legacy = "com.evandev.reliable_remover.util.legacy"

        regex(eval(node.metadata.version, "<1.20")) {
            fun swap(from: String, to: String) = replace(Regex.escape(from), to, Regex.escape(to), from)
            swap("import net.minecraft.world.level.storage.loot.LootParams;", "import $legacy.LootParams;")
            for (receiver in listOf("entity", "player", "this", "trader")) {
                swap("$receiver.level()", "$receiver.getLevel()")
            }
        }

        regex(eval(node.metadata.version, "<1.19.3")) {
            fun swap(from: String, to: String) = replace(Regex.escape(from), to, Regex.escape(to), from)
            swap("import net.minecraft.core.registries.BuiltInRegistries;", "import $legacy.BuiltInRegistries;")
            swap("import net.minecraft.core.registries.Registries;", "import $legacy.Registries;")
            swap("import net.minecraft.core.HolderLookup;", "import $legacy.HolderLookup;")
            for (registry in listOf("ITEM", "BLOCK", "FLUID", "POTION", "MOB_EFFECT", "ENCHANTMENT")) {
                swap("BuiltInRegistries.$registry.wrapAsHolder(", "BuiltInRegistries.wrapAsHolder(BuiltInRegistries.$registry, ")
            }
        }
    }
}

stonecutter tasks {
    order("publishModrinth")
    order("publishCurseforge")
}

for (version in stonecutter.versions.map { it.version }.distinct()) tasks.register("publish$version") {
    group = "publishing"
    dependsOn(stonecutter.tasks.named("publishMods") { metadata.version == version })
}
