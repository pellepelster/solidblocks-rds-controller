plugins {
    id("de.solidblocks.rds.kotlin-library-conventions")
}

dependencies {
    implementation("org.liquibase:liquibase-core:4.6.2")

    implementation("com.zaxxer:HikariCP:3.4.5")
    api("org.jooq:jooq:3.16.6")
    runtimeOnly("com.mattbertolini:liquibase-slf4j:4.0.0")
}
