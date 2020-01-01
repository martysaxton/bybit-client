import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.61"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "io.xooxo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("ch.qos.logback:logback-classic:1.2.3")
}


configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

task("execute", JavaExec::class) {
    dependsOn("assemble")
    group = "execute"
    main = "io.xooxo.bybit.MainKt"
    classpath = sourceSets["main"].runtimeClasspath
}

tasks {
    jar {
        manifest {
            attributes(mapOf("Main-Class" to "io.xooxo.bybit.MainKt"))
        }
    }
}

