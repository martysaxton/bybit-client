import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        jcenter()
    }
}

plugins {
//    kotlin("jvm")
    kotlin("jvm")

    `maven-publish`
    id("io.gitlab.arturbosch.detekt").version("1.3.1")
}

group = "io.xooxo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api("org.slf4j:slf4j-api:1.7.30")
    api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.14.0") // JVM dependency
    api("org.http4k:http4k-core:3.205.0")
    api("org.http4k:http4k-client-okhttp:3.205.0")
    api("org.http4k:http4k-format-jackson:3.205.0")
    api("org.http4k:http4k-client-websocket:3.205.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
    testImplementation("ch.qos.logback:logback-classic:1.2.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")

}


sourceSets {
    create("integrationTest") {
        withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
            kotlin.srcDir("src/integrationTest/kotlin")
            resources.srcDir("src/integrationTest/resources")
            compileClasspath += sourceSets["main"].output + sourceSets["test"].runtimeClasspath
            runtimeClasspath += output + compileClasspath + sourceSets["test"].runtimeClasspath
        }
    }
}

task<Test>("integrationTest") {
    description = "Runs the integration tests"
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    mustRunAfter(tasks["test"])
    useJUnitPlatform()
    reports.html.isEnabled = true
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

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    reports.html.isEnabled = true
}


detekt {
    //    failFast = false // fail build on any finding
    buildUponDefaultConfig = true // preconfigure defaults
    config = files("$projectDir/detekt.yml") // point to your custom config defining rules to run, overwriting default behavior

//    reports {
//        html.enabled = true // observe findings in your browser with structure and code snippets
//        xml.enabled = true // checkstyle like format mainly for integrations like Jenkins
//        txt.enabled = true // similar to the console output, contains issue signature to manually edit baseline files
//    }
}

tasks {
    jar {
        manifest {
            attributes(mapOf("Main-Class" to "io.xooxo.bybit.MainKt"))
        }
    }
}

