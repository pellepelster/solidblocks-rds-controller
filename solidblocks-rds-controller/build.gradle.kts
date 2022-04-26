plugins {
    id("de.solidblocks.rds.kotlin-application-conventions")
}

dependencies {
    implementation(project(":solidblocks-rds-shared"))
    implementation(project(":solidblocks-rds-controller-model"))
}

application {
    mainClass.set("de.solidblocks.rds.controller.ApplicationCliKt")
}
