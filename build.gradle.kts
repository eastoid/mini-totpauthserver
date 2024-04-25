import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.23"
    id("com.google.devtools.ksp") version "1.9.23-1.0.19"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.micronaut.application") version "4.3.5"
    id("io.micronaut.aot") version "4.3.5"
    kotlin("plugin.serialization") version "1.9.22"
}

version = "1"
group = "totpauthserver"

val kotlinVersion=project.properties.get("kotlinVersion")
repositories {
    mavenCentral()
}

dependencies {
    ksp("io.micronaut:micronaut-http-validation:4.3.5")
    ksp("io.micronaut.serde:micronaut-serde-processor:2.9.0")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime:4.3.0")
    implementation("io.micronaut.serde:micronaut-serde-jackson:2.9.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    compileOnly("io.micronaut:micronaut-http-client:4.3.5")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.2")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
    testImplementation("io.micronaut:micronaut-http-client:4.3.5")

    implementation("com.atlassian:onetime:2.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.micronaut.views:micronaut-views-thymeleaf:5.2.0")
    implementation("org.thymeleaf:thymeleaf:3.1.2.RELEASE")
}


application {
    mainClass.set("totpauthserver.ApplicationKt")
}
java {
    sourceCompatibility = JavaVersion.toVersion("21")
}


graalvmNative.toolchainDetection.set(false)
micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("micronauttest.*")
    }
    aot {
    // Please review carefully the optimizations enabled below
    // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading.set(false)
        convertYamlToJava.set(false)
        precomputeOperations.set(true)
        cacheEnvironment.set(true)
        optimizeClassLoading.set(true)
        deduceEnvironment.set(true)
        optimizeNetty.set(true)
    }
}

//tasks.named<io.micronaut.gradle.docker.MicronautDockerfile>("dockerfile") {
//    baseImage("eclipse-temurin:21-jre-jammy")
//}

//tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
//    jdkVersion.set("21")
//}


tasks.withType<ShadowJar> {
    archiveFileName.set("tas.jar")
    archiveClassifier = ""
    manifest.attributes("Main-Class" to "micronauttest.ApplicationKt")

    mergeServiceFiles()
    isZip64 = true
}