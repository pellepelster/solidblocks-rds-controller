import Versions.vertX

plugins {
    id("de.solidblocks.rds.kotlin-application-conventions")
}

dependencies {
    implementation(project(":solidblocks-rds-backend-base"))
    implementation(project(":solidblocks-rds-shared"))
    implementation(project(":solidblocks-rds-model-shared"))
    implementation(project(":solidblocks-rds-controller-model"))
    implementation(project(":solidblocks-rds-cloud-init"))
    implementation(project(":solidblocks-rds-postgresql-agent"))

    implementation("io.vertx:vertx-web:$vertX")
    implementation("io.vertx:vertx-lang-kotlin:$vertX")

    api("net.javacrumbs.shedlock:shedlock-provider-jdbc:4.32.0")
    api("com.github.kagkarlsson:db-scheduler:10.5")

    implementation("io.github.resilience4j:resilience4j-kotlin:1.7.1")
    implementation("io.github.resilience4j:resilience4j-retry:1.7.1")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0-rc3")

    implementation("me.tomsdevsn:hetznercloud-api:2.15.1")

    testImplementation(project(":solidblocks-rds-postgresql-agent"))
    testImplementation(project(":solidblocks-rds-test"))

    testImplementation("io.rest-assured:rest-assured:5.1.1")
    testImplementation("io.rest-assured:json-path:5.1.1")
    testImplementation("io.rest-assured:kotlin-extensions:5.1.1")
}

application {
    mainClass.set("de.solidblocks.rds.controller.ApplicationCliKt")
}
