import Versions.vertX

plugins {
    id("de.solidblocks.rds.kotlin-application-conventions")
    id("solidblocks.rds.kotlin-publish-conventions")
}

dependencies {
    implementation(project(":solidblocks-rds-shared"))

    implementation("io.vertx:vertx-web:$vertX")
    implementation("io.vertx:vertx-lang-kotlin:$vertX")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")

    implementation("io.github.hakky54:sslcontext-kickstart:7.2.0")
    implementation("io.github.hakky54:sslcontext-kickstart-for-pem:7.2.0")

    implementation("com.jcabi:jcabi-manifests:1.1")
}

application {
    mainClass.set("de.solidblocks.rds.agent.AgentKt")
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            artifact(tasks.distTar)
        }
    }
}

tasks.getByName("check").dependsOn(":solidblocks-rds-postgresql:docker")
