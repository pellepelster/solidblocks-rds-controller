import Versions.junitJupiterVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("de.solidblocks.rds.kotlin-library-conventions")
}

dependencies {

    implementation(project(":solidblocks-rds-backend-base"))

    implementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    implementation(kotlin("stdlib-jdk8"))
}
repositories {
    mavenCentral()
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
