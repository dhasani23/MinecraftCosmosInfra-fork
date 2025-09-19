/*
 * This file configures the build for the 'lambda' subproject.
 * It applies the common-conventions plugin to inherit shared build configuration,
 * the shadow plugin for creating a fat JAR, and the Kotlin serialization plugin.
 * The subproject includes dependencies specific to AWS Lambda development,
 * such as the AWS Lambda Java core and events libraries, and the AWS SDK for SNS, EC2, and ECS.
 * It also includes the kotlinx-serialization-json library for JSON serialization.
 */

plugins {
    // Apply the common convention plugin for shared build configuration between library and application projects.
    id("MinecraftCosmosInfra.kotlin-common-conventions")

    application
    id("com.github.johnrengelman.shadow") version "7.0.0"

}

dependencies {
    // Common libs
    implementation(project(":common"))

    // CLI libraries
    implementation("info.picocli:picocli:4.6.3")
    kapt ("info.picocli:picocli-codegen:4.6.3")

    // Web server
    implementation("io.ktor:ktor-server-core:2.3.10")
    implementation("io.ktor:ktor-server-netty:2.3.10")

    // AWS
    implementation("software.amazon.awssdk:s3:2.21.42")
    implementation("software.amazon.awssdk:aws-core:2.21.42") // Core utilities
    implementation("software.amazon.awssdk:ec2:2.21.42")
    implementation("software.amazon.awssdk:ecs:2.21.42")
    implementation("software.amazon.awssdk:dynamodb-enhanced:2.25.31")

    // Apache commons I/O for file utils
    implementation("commons-io:commons-io:2.11.0")
}

application {
    mainClass.set("com.lilaceclipse.cosmos.docker.MainKt")
}