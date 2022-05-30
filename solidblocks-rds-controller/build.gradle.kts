plugins {
    id("de.solidblocks.rds.kotlin-application-conventions")
}

dependencies {
    implementation(project(":solidblocks-rds-shared"))
    implementation(project(":solidblocks-rds-controller-model"))

    implementation("io.vertx:vertx-web:4.2.3")
    implementation("io.vertx:vertx-lang-kotlin:4.2.3")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")

    implementation("me.tomsdevsn:hetznercloud-api:2.13.0")
}

application {
    mainClass.set("de.solidblocks.rds.controller.ApplicationCliKt")
}
