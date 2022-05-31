plugins {
    id("de.solidblocks.rds.kotlin-application-conventions")
}

dependencies {
    implementation(project(":solidblocks-rds-base"))
    implementation(project(":solidblocks-rds-shared"))
    implementation(project(":solidblocks-rds-controller-model"))

    implementation("io.vertx:vertx-web:4.2.3")
    implementation("io.vertx:vertx-lang-kotlin:4.2.3")

    api("net.javacrumbs.shedlock:shedlock-provider-jdbc:4.32.0")
    api("com.github.kagkarlsson:db-scheduler:10.5")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")

    implementation("me.tomsdevsn:hetznercloud-api:2.13.0")

    implementation("org.bouncycastle:bcprov-jdk15to18:1.68")
    implementation("net.i2p.crypto:eddsa:0.3.0")

    testImplementation(project(":solidblocks-rds-test"))

}

application {
    mainClass.set("de.solidblocks.rds.controller.ApplicationCliKt")
}
