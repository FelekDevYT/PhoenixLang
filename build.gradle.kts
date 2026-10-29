plugins {
    id("java")
    antlr
    kotlin("jvm")
}

group = "me.felek.bright"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val antlrVersion = "4.13.2"

dependencies {
    antlr("org.antlr:antlr4:$antlrVersion")
    implementation("org.antlr:antlr4-runtime:$antlrVersion")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(kotlin("test"))

    implementation("com.github.ajalt.clikt:clikt:5.0.1")

    implementation(platform("org.ow2.asm:asm-bom:9.11"))
    implementation("org.ow2.asm:asm")
    implementation("org.ow2.asm:asm-tree")
    implementation("org.ow2.asm:asm-analysis")
    implementation("org.ow2.asm:asm-commons")
    implementation("org.ow2.asm:asm-util")
}

tasks.generateGrammarSource {
    arguments = arguments + listOf("-visitor", "-no-listener")
    arguments = arguments + listOf("-package", "me.felek.bright.parser")
    outputDirectory = file("${layout.buildDirectory.get()}/generated-src/antlr/main/me/felek/bright/parser")
}

tasks.compileJava {
    dependsOn(tasks.generateGrammarSource)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}