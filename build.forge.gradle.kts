plugins {
    id("net.minecraftforge.gradle")
    id ("dev.kikugie.postprocess.jsonlang")
    id("me.modmuss50.mod-publish-plugin")
}

tasks.named<ProcessResources>("processResources") {
    fun prop(name: String) = project.property(name) as String

    val props = HashMap<String, String>().apply {
        this["version"] = prop("mod.version") + "-" + prop("deps.minecraft")
        this["minecraft"] = prop("mod.mc_dep_forgelike")
        this["forge_version"] = prop("deps.forge")
        this["fabric_api_version"] = prop("deps.forgified-fabric-api")
        this["mod_name"] = prop("mod.name")
        this["mod_description"] = prop("mod.description")
        this["mod_license"] = prop("mod.license")
    }

    filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml", "META-INF/mods.toml")) {
        expand(props)
    }

    val mixin = HashMap<String, String>().apply {
        this["java"] = "JAVA_${prop("deps.java")}"
    }

    filesMatching(listOf("atlas-core.mixins.json")) {
        expand(mixin)
    }
}

version = "${property("mod.version")}.${property("mod.sub_version")}-${property("deps.minecraft")}-forge"
base.archivesName = property("mod.archives_base") as String

jsonlang {
    languageDirectories = listOf("assets/${property("mod.id")}/lang")
    prettyPrint = true
}

repositories {
    maven {
        name = "Sinytra"
        url = uri("https://maven.su5ed.dev/releases")
    }
    maven {
        name = "shedaniel (Cloth Config)"
        url = uri("https://maven.shedaniel.me/")
        content {
            includeGroupAndSubgroups("me.shedaniel")
        }
    }
    maven {
        name = "Terraformers (Mod Menu)"
        url = uri("https://maven.terraformersmc.com/releases/")
        content {
            includeGroupAndSubgroups("com.terraformersmc")
            includeGroupAndSubgroups("dev.emi")
        }
    }
    maven {
        name = "Wisp Forest Maven"
        url = uri("https://maven.wispforest.io/releases/")
        content {
            includeGroupAndSubgroups("io.wispforest")
        }
    }
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
        content {
            includeGroupAndSubgroups("maven.modrinth")
        }
    }
    maven {
        name = "WTHIT"
        url = uri("https://maven2.bai.lol")
        content {
            includeGroupAndSubgroups("mcp.mobius.waila")
            includeGroupAndSubgroups("lol.bai")
        }
    }
    maven {
        name = "Sisby Maven"
        url = uri("https://repo.sleeping.town/")
        content {
            includeGroupAndSubgroups("folk.sisby")
        }
    }
    maven {
        name = "Parchment Mappings"
        url = uri("https://maven.parchmentmc.org")
        content {
            includeGroupAndSubgroups("org.parchmentmc")
        }
    }
    maven {
        name = "Xander Maven"
        url = uri("https://maven.isxander.dev/releases")
        content {
            includeGroupAndSubgroups("dev.isxander")
            includeGroupAndSubgroups("org.quiltmc.parsers")
        }
    }
    maven {
        name = "Nucleoid Maven (Polymer/Trinkets)"
        url = uri("https://maven.nucleoid.xyz")
        content {
            includeGroupAndSubgroups("eu.pb4")
            includeGroupAndSubgroups("xyz.nucleoid")
        }
    }
    maven {
        name = "Fuzs Mod Resources"
        url = uri("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
        content {
            includeGroupAndSubgroups("fuzs")
        }
    }
    maven {
        name = "Kotlin For Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
        content {
            includeGroupAndSubgroups("thedarkcolour")
        }
    }
    exclusiveContent {
        forRepository {
            maven {
              name = "Cassian's Maven"
              url = uri("https://maven.cassian.cc")
            }
        }
        filter {
            includeGroupAndSubgroups("cc.cassian")
        }
    }
    mavenCentral()
}

minecraft {
    version = property("deps.forge") as String

    if (hasProperty("deps.parchment")) {
        mappings("parchment", property("deps.parchment") as String)
    } else if (stonecutter.eval(stonecutter.current.version, "<26")) {
        mappings("official", property("deps.minecraft") as String)
    }

    runs {
        configureEach {
            workingDir = file("run/")

//            systemProperty 'eventbus.api.strictRuntimeChecks', 'true'
//            systemProperty 'forge.enabledGameTestNamespaces', 'atlas_core'
        }

        register("client")
        register("server")
    }
    sourceSets["main"].resources.srcDir("src/main/generated")
}

dependencies {
    minecraft.dependency("net.minecraftforge:forge:${property("deps.minecraft")}-${property("deps.forge")}")
    implementation("net.minecraftforge:forge:${property("deps.minecraft")}-${property("deps.forge")}")
    annotationProcessor("net.minecraftforge:eventbus-validator:7.0.1")
    if (hasProperty("deps.mixinextras")) {
        compileOnly("io.github.llamalad7:mixinextras-common:${property("deps.mixinextras")}")
        annotationProcessor("io.github.llamalad7:mixinextras-common:${property("deps.mixinextras")}")
        jarJar("io.github.llamalad7:mixinextras-forge:${property("deps.mixinextras")}")
        implementation("io.github.llamalad7:mixinextras-forge:${property("deps.mixinextras")}")
    }
    implementation("org.sinytra.forgified-fabric-api:forgified-fabric-api:${property("deps.forgified-fabric-api")}")
    api("me.shedaniel.cloth:cloth-config-forge:${property("deps.cloth-config")}")
}


tasks {
    processResources {
        exclude("**/fabric.mod.json", "**/*.classtweaker", "**/neoforge.mods.toml")
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
}

java {
    withSourcesJar()
    val javaCompat = if (stonecutter.eval(stonecutter.current.version, ">=26")) {
        JavaVersion.VERSION_25
    } else if (stonecutter.eval(stonecutter.current.version, ">=1.20.5")) {
        JavaVersion.VERSION_21
    } else {
        JavaVersion.VERSION_17
    }
    sourceCompatibility = javaCompat
    targetCompatibility = javaCompat
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

    type = BETA
    displayName = "${property("mod.name")} ${property("mod.version")} for ${stonecutter.current.version} Forge"
    version = "${property("mod.version")}+${property("deps.minecraft")}-forge"
    changelog = provider { rootProject.file("CHANGELOG-LATEST.md").readText() }
    modLoaders.add("neoforge")

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = env.MODRINTH_API_KEY.orNull()
        minecraftVersions.add(property("deps.minecraft") as String)
        minecraftVersions.addAll(additionalVersions)
        optional("mcqoy")
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = env.CURSEFORGE_API_KEY.orNull()
        minecraftVersions.add(property("deps.minecraft") as String)
        minecraftVersions.addAll(additionalVersions)
    }
}
