pluginManagement {
    plugins {
        // Update this in libs.version.toml when you change it here
        kotlin("jvm") version "1.7.10"
        kotlin("plugin.serialization") version "1.7.10"

        id("com.github.johnrengelman.shadow") version "7.1.0"
    }
}

rootProject.name = "XKCDBot"

enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}
