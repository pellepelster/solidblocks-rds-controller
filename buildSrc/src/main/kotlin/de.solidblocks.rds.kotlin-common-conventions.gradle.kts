import Versions.junitJupiterVersion
import Versions.testContainersVersion
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("net.nemerosa.versioning")
    id("org.jlleitschuh.gradle.ktlint")
}

repositories {
    mavenCentral()
}

val versionFile = File("$rootDir/version.txt")
version = System.getenv("SOLIDBLOCKS_VERSION") ?: versionFile.readText().trim()

dependencies {
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("io.github.microutils:kotlin-logging-jvm:2.0.6")
    implementation("ch.qos.logback:logback-classic:1.2.10")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testImplementation("org.assertj:assertj-core:3.22.0")

    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")

    testImplementation("io.mockk:mockk:1.12.4")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        this.showStandardStreams = true
        events = setOf(
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED,
            TestLogEvent.STANDARD_OUT,
            TestLogEvent.STANDARD_ERROR
        )
    }
}

tasks.jar {
    manifest {
        attributes["Commit"] = versioning.info.commit
        attributes["Solidblocks-Version"] = version
    }
}

configure<KtlintExtension> {
    outputToConsole.set(true)
    outputColorName.set("RED")
    disabledRules.set(listOf("no-wildcard-imports", "redundant-curly-braces"))
    ignoreFailures.set(true)

    reporters {
        reporter(ReporterType.PLAIN_GROUP_BY_FILE)
    }
    filter {
        exclude("**/build/**")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_16.toString()
        targetCompatibility = JavaVersion.VERSION_16.toString()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}