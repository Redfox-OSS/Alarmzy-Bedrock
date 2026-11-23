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
        // Bedrock design system, published from github.com/Redfox-OSS/bedrock.
        maven("https://redfox-oss.github.io/bedrock/maven") {
            content { includeGroup("dev.redfox") }
        }
    }
}

rootProject.name = "Alarmzy-Bedrock"
include(":app")

// While changing Bedrock and this app together, build with -PlocalBedrock (or put
// localBedrock=true in ~/.gradle/gradle.properties) to use a sibling checkout at ../Bedrock in
// place of the published dev.redfox:bedrock; its changes show up here on the next build.
if (providers.gradleProperty("localBedrock").isPresent) {
    includeBuild("../Bedrock") {
        dependencySubstitution {
            substitute(module("dev.redfox:bedrock")).using(project(":bedrock"))
        }
    }
}
