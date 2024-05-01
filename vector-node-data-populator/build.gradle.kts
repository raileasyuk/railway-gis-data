plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
}

group = "uk.co.raileasy.gis"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/mil.nga.geopackage/geopackage-core
    implementation("mil.nga.geopackage:geopackage-core:6.6.7")
    // https://mvnrepository.com/artifact/mil.nga.geopackage/geopackage
    implementation("mil.nga.geopackage:geopackage:6.6.5")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
}

tasks.test {
    useJUnitPlatform()
}