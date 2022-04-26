plugins {
    id("de.solidblocks.rds.kotlin-application-conventions")
}

dependencies {
    implementation(project(":solidblocks-rds-shared"))
}

application {
    mainClass.set("de.solidblocks.rds.controller.ControllerKt")
}
