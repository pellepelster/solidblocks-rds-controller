import Versions.vertX
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("de.solidblocks.rds.kotlin-library-conventions")
}

dependencies {
    api("io.vertx:vertx-web:$vertX")
    api("io.vertx:vertx-lang-kotlin:$vertX")
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
