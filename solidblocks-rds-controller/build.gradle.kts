import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("de.solidblocks.rds.kotlin-application-conventions")
}

dependencies {
    implementation(project(":solidblocks-rds-base"))
    implementation(project(":solidblocks-rds-backend-base"))
    implementation(project(":solidblocks-rds-shared"))
    implementation(project(":solidblocks-rds-model-shared"))
    implementation(project(":solidblocks-rds-controller-model"))
    implementation(project(":solidblocks-rds-cloud-init"))
    implementation(project(":solidblocks-rds-postgresql-agent"))

    api("net.javacrumbs.shedlock:shedlock-provider-jdbc:5.0.1")
    api("com.github.kagkarlsson:db-scheduler:11.6")

    implementation("io.github.resilience4j:resilience4j-kotlin:2.0.2")
    implementation("io.github.resilience4j:resilience4j-retry:2.0.2")

    implementation("me.tomsdevsn:hetznercloud-api:3.0.1")

    testImplementation(project(":solidblocks-rds-postgresql-agent"))
    testImplementation(project(":solidblocks-rds-test"))

    testImplementation("io.rest-assured:rest-assured:5.3.0")
    testImplementation("io.rest-assured:json-path:5.3.0")
    testImplementation("io.rest-assured:kotlin-extensions:5.3.0")
    implementation(kotlin("stdlib-jdk8"))
}

application {
    mainClass.set("de.solidblocks.rds.controller.ApplicationCliKt")
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
