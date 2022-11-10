import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("de.solidblocks.rds.kotlin-application-conventions")
    id("de.solidblocks.rds.kotlin-common-conventions")
    id("de.solidblocks.rds.kotlin-publish-conventions")
}

dependencies {
    implementation(project(":solidblocks-rds-base"))
    implementation(project(":solidblocks-rds-shared"))
    implementation(project(":solidblocks-rds-model-shared"))

    implementation("com.github.docker-java:docker-java-core:3.2.12")
    implementation("com.github.docker-java:docker-java-transport-zerodep:3.2.12")

    implementation("org.postgresql:postgresql:42.3.8")

    implementation("io.github.resilience4j:resilience4j-kotlin:1.7.1")
    implementation("io.github.resilience4j:resilience4j-retry:1.7.1")

    implementation("io.github.hakky54:sslcontext-kickstart:7.2.0")
    implementation("io.github.hakky54:sslcontext-kickstart-for-pem:7.2.0")

    testImplementation(project(":solidblocks-rds-shared"))
    testImplementation(project(":solidblocks-rds-backend-base"))
    implementation(kotlin("stdlib-jdk8"))
}

application {
    mainClass.set("de.solidblocks.rds.postgresql.agent.PostgresqlAgentCommandKt")
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            artifact(tasks.distTar)
        }
    }
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
