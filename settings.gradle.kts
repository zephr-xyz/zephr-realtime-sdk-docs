pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("com.google.cloud.artifactregistry.gradle-plugin") version "2.2.5"
}

rootProject.name = "ZephrSampleClientApp"
include(":app")
