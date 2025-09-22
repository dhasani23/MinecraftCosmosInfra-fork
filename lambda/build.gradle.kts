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

    id("com.github.johnrengelman.shadow") version "7.0.0"
    kotlin("plugin.serialization")
}

dependencies {
    // Common libs
    implementation(project(":common"))

    // Lambda
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.4")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // AWS client dependencies
    implementation("com.amazonaws:aws-java-sdk-sns:1.12.691")
    implementation("software.amazon.awssdk:ec2:2.21.42")
    implementation("com.amazonaws:aws-java-sdk-ecs:1.12.691")
    implementation("software.amazon.awssdk:dynamodb-enhanced:2.25.31")
}
