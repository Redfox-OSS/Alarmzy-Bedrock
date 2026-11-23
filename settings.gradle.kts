pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Alarmzy-Bedrock"
include(":app")

// The Bedrock design system (github.com/Redfox-OSS/bedrock), built from a sibling checkout:
// clone it next to this repository (../Bedrock). Gradle builds the library from source and uses
// it in place of dev.redfox:bedrock, so a change in Bedrock shows up here on the next build.
includeBuild("../Bedrock") {
    dependencySubstitution {
        substitute(module("dev.redfox:bedrock")).using(project(":bedrock"))
    }
}
