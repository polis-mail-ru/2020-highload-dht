import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    application
    id("net.ltgt.errorprone") version "0.8.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    jcenter()
}

dependencies {
    // Checks
    errorprone("com.google.errorprone:error_prone_core:2.3.3")

    // Our beloved one-nio
    compile("ru.odnoklassniki:one-nio:1.3.1")

    // Logging
    compile("org.slf4j:slf4j-api:1.7.26")
    compile("ch.qos.logback:logback-classic:1.2.3")

    // Annotations for better code documentation
    compile("com.intellij:annotations:12.0")

    // Guava primitives
    compile("com.google.guava:guava:27.0.1-jre")

    // JUnit Jupiter test framework
    testCompile("org.junit.jupiter:junit-jupiter-api:5.4.0")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:5.4.0")
}

val run by tasks.getting(JavaExec::class) {
    standardInput = System.`in`
}

tasks {
    test {
        maxHeapSize = "256m"
        useJUnitPlatform()
    }
}

application {
    // Define the main class for the application
    mainClassName = "ru.mail.polis.Cluster"

    // And limit Xmx
    applicationDefaultJvmArgs = listOf("-Xmx256m")
}

// Fail on warnings
tasks.withType<JavaCompile> {
    val compilerArgs = options.compilerArgs
    compilerArgs.add("-Werror")
    compilerArgs.add("-Xlint:all")
}

// Error prone options
tasks.named<JavaCompile>("compileTestJava") {
    options.errorprone.isEnabled.set(false)
}