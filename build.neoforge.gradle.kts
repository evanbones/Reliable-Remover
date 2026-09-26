plugins {
    id("net.neoforged.moddev")
    id("dev.kikugie.postprocess.jsonlang")
    id("me.modmuss50.mod-publish-plugin")
    id("maven-publish")
}

val minecraft = stonecutter.current.version
val mcVersion = stonecutter.current.project.substringBeforeLast('-')

tasks.named<ProcessResources>("processResources") {
    fun prop(name: String) = project.property(name) as String

    val props = HashMap<String, String>().apply {
        this["version"] = prop("mod.version") + "+" + prop("deps.minecraft")
        this["minecraft_version_range"] = prop("mod.mc_dep_forgelike")
        this["mod_id"] = prop("mod.id")
        this["mod_name"] = prop("mod.name")
        this["description"] = prop("mod.description")
        this["mod_author"] = prop("mod.author")
        this["credits"] = prop("mod.credits")
        this["license"] = prop("mod.license")
        this["neoforge_loader_version_range"] = prop("deps.neoforge_loader_version_range")
        this["neoforge_version"] = prop("deps.neoforge")
        this["java_version"] = prop("deps.java_version")
        this["yacl_version"] = prop("deps.yacl").substringBefore('+')
    }

    filesMatching(listOf("neoforge.mod.json", "META-INF/neoforge.mods.toml", "META-INF/mods.toml", "*.mixins.json")) {
        expand(props)
    }
}

version = "${property("mod.version")}+${property("deps.minecraft")}-neoforge"
base.archivesName = property("mod.id") as String

jsonlang {
    languageDirectories = listOf("assets/${property("mod.id")}/lang")
    prettyPrint = true
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "Terraformers (Mod Menu, EMI)"
        url = uri("https://maven.terraformersmc.com/releases/")
        content {
            includeGroupAndSubgroups("com.terraformersmc")
            includeGroupAndSubgroups("dev.emi")
        }
    }
    maven {
        name = "Xander Maven (YACL)"
        url = uri("https://maven.isxander.dev/releases")
        content {
            includeGroupAndSubgroups("dev.isxander")
            includeGroupAndSubgroups("org.quiltmc.parsers")
        }
    }
    maven {
        name = "Quilt Maven"
        url = uri("https://maven.quiltmc.org/repository/release/")
        content {
            includeGroupAndSubgroups("org.quiltmc.parsers")
        }
    }
    maven {
        name = "Cassian's Maven"
        url = uri("https://maven.cassian.cc/")
        content {
            includeGroupAndSubgroups("cc.cassian")
        }
    }
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
        content {
            includeGroupAndSubgroups("maven.modrinth")
        }
    }
}

neoForge {
    enable {
        version = property("deps.neoforge") as String
        isDisableRecompilation = true
    }
    validateAccessTransformers = true

    runs {
        configureEach {
            systemProperty("neoforge.warnings.onlyin.hide", "true")
            systemProperty("kotlinx.coroutines.debug", "off")
        }
        register("client") {
            gameDirectory = file("run/")
            client()
        }
        register("server") {
            gameDirectory = file("run/")
            server()
        }
    }

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets["main"])
        }
    }
}

tasks {
    processResources {
        exclude("**/fabric.mod.json", "**/*.accesswidener", "**/mods.toml", "**/*.fabric.mixins.json")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }

    withType<JavaExec>().configureEach {
        jvmArgs("-Dkotlinx.coroutines.debug=off")
    }
}

val localRuntime: Configuration by configurations.creating
configurations.runtimeClasspath { extendsFrom(localRuntime) }

dependencies {
    // Reliable Recipes
    implementation("maven.modrinth:reliable-recipes:${property("deps.reliable_recipes")}-neoforge")

    // YACL
    if (stonecutter.eval(minecraft, ">=26.1")) {
        compileOnly("dev.isxander:yet-another-config-lib:${property("deps.yacl")}")
        localRuntime("dev.isxander:yet-another-config-lib:${property("deps.yacl")}")
    } else {
        compileOnly("maven.modrinth:yacl:${property("deps.yacl")}")
        localRuntime("maven.modrinth:yacl:${property("deps.yacl")}")
    }

    // RRV or EMI
    if (stonecutter.eval(minecraft, ">=26.1")) {
        implementation("cc.cassian.rrv:reliable-recipe-viewer-neoforge:${property("deps.rrv")}")
    } else {
        compileOnly("dev.emi:emi-neoforge:${property("deps.emi")}")
        "localRuntime"("dev.emi:emi-neoforge:${property("deps.emi")}")
    }

    // Mixin Constraints
    compileOnly("com.moulberry:mixinconstraints:${property("deps.mixin_constraints")}")
    val mixinConstraints = implementation("com.moulberry:mixinconstraints") {
        version {
            strictly("[${property("deps.mixin_constraints")},)")
            prefer(property("deps.mixin_constraints") as String)
        }
    }
    "jarJar"(mixinConstraints!!)
}

val javaVer = (property("deps.java_version") as String).toInt()
java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVer)
    sourceCompatibility = JavaVersion.toVersion(javaVer)
    targetCompatibility = JavaVersion.toVersion(javaVer)
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = javaVer
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = property("mod.group") as String
            artifactId = "${property("mod.id")}-neoforge"
            version = "${property("mod.version")}+${property("deps.minecraft")}"

            from(components["java"])
        }
    }
}

val additionalVersionsStr = findProperty("publish.additionalVersions") as String?
val additionalVersions: List<String> = additionalVersionsStr
    ?.split(",")
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() }
    ?: emptyList()

publishMods {
    file = tasks.jar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.named<org.gradle.jvm.tasks.Jar>("sourcesJar").map { it.archiveFile.get() })

    type = STABLE
    displayName = "${property("mod.name")} NeoForge ${stonecutter.current.project.substringBeforeLast('-')} - ${property("mod.version")}"
    version = "${property("mod.version")}+${property("deps.minecraft")}-neoforge"
    changelog = provider { rootProject.file("CHANGELOG-LATEST.md").readText() }
    modLoaders.add("neoforge")

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = providers.environmentVariable("MODRINTH_TOKEN").orElse(providers.environmentVariable("MODRINTH_API_KEY"))
        minecraftVersions.add(property("deps.minecraft") as String)
        minecraftVersions.addAll(additionalVersions)
        requires("reliable-recipes")
        optional("yacl")
        optional("rrv")
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN").orElse(providers.environmentVariable("CURSEFORGE_API_KEY"))
        minecraftVersions.add(property("deps.minecraft") as String)
        minecraftVersions.addAll(additionalVersions)
        requires("reliable-recipes")
        optional("yacl")
        optional("rrv")
        client = true
        server = true
    }
}
