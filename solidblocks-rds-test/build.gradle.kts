import Versions.junitJupiterVersion

plugins {
    id("de.solidblocks.rds.kotlin-library-conventions")
}

dependencies {

    implementation(project(":solidblocks-rds-backend-base"))

    implementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
}
