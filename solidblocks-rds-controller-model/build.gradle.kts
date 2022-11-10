import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("de.solidblocks.rds.kotlin-library-conventions")
    id("nu.studer.jooq") version "7.1.1"
}

dependencies {

    implementation(project(":solidblocks-rds-backend-base"))

    implementation("org.apache.derby:derbytools:10.15.2.0")
    implementation("org.apache.derby:derby:10.15.2.0")

    implementation("org.yaml:snakeyaml:1.32")

    jooqGenerator("org.jooq:jooq-meta-extensions-liquibase")
    jooqGenerator("org.liquibase:liquibase-core")
    jooqGenerator("org.yaml:snakeyaml:1.32")
    jooqGenerator("org.slf4j:slf4j-jdk14:1.7.30")
    jooqGenerator(files("src/main/resources"))

    testImplementation(project(":solidblocks-rds-test"))
    testImplementation("org.hamcrest:hamcrest:2.2")
    implementation(kotlin("stdlib-jdk8"))
}

jooq {
    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(false)

            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN

                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"

                    target = org.jooq.meta.jaxb.Target().withPackageName("de.solidblocks.rds.controller.model")

                    database.apply {
                        name = "org.jooq.meta.extensions.liquibase.LiquibaseDatabase"

                        properties.add(
                            org.jooq.meta.jaxb.Property().withKey("scripts")
                                .withValue("/db/changelog/db.changelog-master.yaml")
                        )
                        properties.add(
                            org.jooq.meta.jaxb.Property().withKey("includeLiquibaseTables").withValue("false")
                        )
                    }
                }
            }
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
