plugins {
    eclipse
    idea
    `maven-publish`
    id("net.minecraftforge.gradle") version "[6.0.36,6.2)"
}

val baseGroup: String by project
val mcVersion = "1.21"
val modVersion: String by project
val modid: String by project
val jarName: String by project

version = modVersion
group = baseGroup

base {
    archivesName.set(modid)
}

// Mojang ships Java 21 to end users in 1.20.5+
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

println("Java: ${System.getProperty("java.version")}, JVM: ${System.getProperty("java.vm.version")} (${System.getProperty("java.vendor")}), Arch: ${System.getProperty("os.arch")}")

minecraft {
    // Official mappings for 1.21
    mappings("official", "1.21")
    
    // Forge 1.20.6+ uses official mappings at runtime
    reobf = false
    
    copyIdeResources.set(true)
    
    // Access transformer
    val transformerFile = file("src/main/resources/META-INF/accesstransformer.cfg")
    if (transformerFile.exists()) {
        println("Installing access transformer")
        accessTransformer(transformerFile)
    }
    
    runs {
        // Common config for all runs
        configureEach {
            workingDirectory(project.file("run"))
            
            property("forge.logging.console.level", "debug")
            property("eventbus.api.strictRuntimeChecks", "true")
            property("forge.enabledGameTestNamespaces", modid)
            
            // Mixin support
            arg("-mixin.config=${modid}.mixins.json")
            
            mods {
                create(modid) {
                    source(sourceSets.main.get())
                }
            }
        }
        
        create("client") {
            property("forge.enabledGameTestNamespaces", modid)
        }
        
        create("server") {
            property("forge.enabledGameTestNamespaces", modid)
            args("--nogui")
        }
        
        create("gameTestServer") {
            property("forge.enabledGameTestNamespaces", modid)
        }
        
        create("data") {
            workingDirectory(project.file("run-data"))
            
            args(
                "--mod", modid,
                "--all",
                "--output", file("src/generated/resources/").absolutePath,
                "--existing", file("src/main/resources/").absolutePath
            )
        }
    }
}

// Include generated resources
sourceSets.main.get().resources {
    srcDir("src/generated/resources")
}

repositories {
    mavenCentral()
    maven {
        name = "Forge"
        url = uri("https://maven.minecraftforge.net")
    }
    maven {
        name = "Minecraft libraries"
        url = uri("https://libraries.minecraft.net")
    }
    maven {
        name = "Sponge"
        url = uri("https://repo.spongepowered.org/repository/maven-public")
        content {
            includeGroupByRegex("org\\.spongepowered.*")
        }
    }
}

dependencies {
    minecraft("net.minecraftforge:forge:1.21-51.0.33")
    
    // EventBus 7 annotation processor for compile-time validation
    annotationProcessor("net.minecraftforge:eventbus-validator:7.0-beta.10")
    
    // Mixin support
    implementation("org.spongepowered:mixin:0.8.5")
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
}

tasks.processResources {
    val replaceProperties = mapOf(
        "minecraft_version" to mcVersion,
        "minecraft_version_range" to "[1.21,1.22)",
        "forge_version" to "51.0.33",
        "forge_version_range" to "[51,)",
        "loader_version_range" to "[2,)",
        "mod_id" to modid,
        "mod_name" to project.name,
        "mod_version" to modVersion,
        "mod_authors" to "xanning",
        "mod_description" to "Myau Utility Client"
    )
    
    inputs.properties(replaceProperties)
    
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) {
        expand(replaceProperties + mapOf("project" to project))
    }
}

tasks.jar {
    archiveClassifier.set("without-deps")
    manifest {
        attributes(
            "Specification-Title" to modid,
            "Specification-Vendor" to "xanning",
            "Specification-Version" to "1",
            "Implementation-Title" to project.name,
            "Implementation-Version" to modVersion,
            "Implementation-Vendor" to "xanning",
            "MixinConfigs" to "${modid}.mixins.json"
        )
    }
    finalizedBy("reobfJar")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// Merge resources and classes into same directory for modules
sourceSets.configureEach {
    val dir = layout.buildDirectory.dir("sourcesSets/$name")
    output.setResourcesDir(dir.get().asFile)
    java.destinationDirectory.set(dir.get().asFile)
}
