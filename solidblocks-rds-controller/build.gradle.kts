import Versions.vertX

plugins {
    id("de.solidblocks.rds.kotlin-application-conventions")
}

dependencies {
    implementation(project(":solidblocks-rds-base"))
    implementation(project(":solidblocks-rds-shared"))
    implementation(project(":solidblocks-rds-controller-model"))

    implementation("io.vertx:vertx-web:$vertX")
    implementation("io.vertx:vertx-lang-kotlin:$vertX")

    api("net.javacrumbs.shedlock:shedlock-provider-jdbc:4.32.0")
    api("com.github.kagkarlsson:db-scheduler:10.5")

    implementation("io.github.resilience4j:resilience4j-kotlin:1.7.1")
    implementation("io.github.resilience4j:resilience4j-retry:1.7.1")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.2.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")

    implementation("me.tomsdevsn:hetznercloud-api:2.13.0")

    implementation("org.bouncycastle:bcprov-jdk15to18:1.70")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")

    testImplementation(project(":solidblocks-rds-agent"))
    testImplementation(project(":solidblocks-rds-test"))

    testImplementation("io.mockk:mockk:1.12.4")
}

application {
    mainClass.set("de.solidblocks.rds.controller.ApplicationCliKt")
}
