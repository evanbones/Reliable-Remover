import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.expand
import org.gradle.kotlin.dsl.maven
import org.gradle.language.jvm.tasks.ProcessResources
import java.util.*

val Project.mod: ModData get() = ModData(this)
fun Project.prop(key: String): String? = findProperty(key)?.toString()
fun String.upperCaseFirst() = replaceFirstChar { if (it.isLowerCase()) it.uppercaseChar() else it }

fun RepositoryHandler.strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
    forRepository { maven(url) { name = alias } }
    filter { groups.forEach(::includeGroup) }
}

fun ProcessResources.properties(files: Iterable<String>, vararg properties: Pair<String, Any>) {
    for ((name, value) in properties) inputs.property(name, value)
    filesMatching(files) {
        expand(properties.toMap())
    }
}

@JvmInline
value class ModData(private val project: Project) {
    val id: String get() = requireNotNull(project.prop("mod.id")) { "Missing 'mod.id'" }
    val name: String get() = requireNotNull(project.prop("mod.name")) { "Missing 'mod.name'" }
    val version: String get() = requireNotNull(project.prop("mod.version")) { "Missing 'mod.version'" }
    val group: String get() = requireNotNull(project.prop("mod.group")) { "Missing 'mod.group'" }

    fun prop(key: String) = requireNotNull(project.prop("mod.$key")) { "Missing 'mod.$key'" }
    fun dep(key: String) = requireNotNull(project.prop("deps.$key")) { "Missing 'deps.$key'" }
}

private val MIXIN_LISTS = listOf("mixins", "client", "server")

@Suppress("UNCHECKED_CAST")
fun pruneMixinConfig(config: java.io.File, classesDirs: Iterable<java.io.File>, sourceRoot: java.io.File, logger: org.gradle.api.logging.Logger) {
    val json = groovy.json.JsonSlurper().parse(config) as MutableMap<String, Any?>
    val packagePath = (json["package"] as String).replace('.', '/')
    for (list in MIXIN_LISTS) {
        val entries = json[list] as? List<String> ?: continue
        json[list] = entries.filter { entry ->
            val path = "$packagePath/${entry.replace('.', '/')}"
            check(java.io.File(sourceRoot, "$path.java").isFile) { "${config.name} lists $entry, which has no source file" }
            val compiled = classesDirs.any { java.io.File(it, "$path.class").isFile }
            if (!compiled) logger.info("Pruned {} from {}: not compiled for this version", entry, config.name)
            compiled
        }
    }
    config.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(json)) + "\n")
}
