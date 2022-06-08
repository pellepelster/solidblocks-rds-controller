import Versions.vertX

plugins {
    id("de.solidblocks.rds.kotlin-application-conventions")
    id("de.solidblocks.rds.kotlin-publish-conventions")
    id("com.palantir.docker") version "0.33.0"
}

dependencies {
    implementation(project(":solidblocks-rds-shared"))
    implementation(project(":solidblocks-rds-model-shared"))

    implementation("com.github.docker-java:docker-java-core:3.2.12")
    implementation("com.github.docker-java:docker-java-transport-zerodep:3.2.12")

    implementation("org.postgresql:postgresql:42.3.4")

    implementation("io.github.resilience4j:resilience4j-kotlin:1.7.1")
    implementation("io.github.resilience4j:resilience4j-retry:1.7.1")

    implementation("io.vertx:vertx-web:$vertX")
    implementation("io.vertx:vertx-lang-kotlin:$vertX")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")

    implementation("io.github.hakky54:sslcontext-kickstart:7.2.0")
    implementation("io.github.hakky54:sslcontext-kickstart-for-pem:7.2.0")

    testImplementation(project(":solidblocks-rds-shared"))
    testImplementation(project(":solidblocks-rds-backend-base"))
}

application {
    mainClass.set("de.solidblocks.rds.postgresql.agent.AgentKt")
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            artifact(tasks.distTar)
        }
    }
}

docker {
    name = "solidblocks-rds-postgres"
    copySpec.from("$projectDir/bin").into("bin")
}

tasks.getByName("check").dependsOn("docker")
