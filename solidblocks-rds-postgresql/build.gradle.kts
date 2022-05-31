plugins {
    id("com.palantir.docker") version "0.33.0"
    id("de.solidblocks.rds.kotlin-library-conventions")
}

dependencies {
    implementation("org.freemarker:freemarker:2.3.31")

    implementation("com.github.docker-java:docker-java-core:3.2.12")
    implementation("com.github.docker-java:docker-java-transport-zerodep:3.2.12")

    implementation("org.postgresql:postgresql:42.3.4")

    implementation("io.github.resilience4j:resilience4j-kotlin:1.7.1")
    implementation("io.github.resilience4j:resilience4j-retry:1.7.1")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")
}

docker {
    name = "solidblocks-rds-postgres"
    copySpec.from("$projectDir/bin").into("bin")
}
