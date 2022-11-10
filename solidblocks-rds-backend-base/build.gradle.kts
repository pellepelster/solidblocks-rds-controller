import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("de.solidblocks.rds.kotlin-library-conventions")
}

dependencies {

    implementation("org.bouncycastle:bcprov-jdk15to18:1.70")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")

    implementation("org.liquibase:liquibase-core:4.6.2")
    implementation("com.zaxxer:HikariCP:3.4.5")
    api("org.jooq:jooq:3.16.6")
    runtimeOnly("com.mattbertolini:liquibase-slf4j:4.0.0")
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
