plugins {
    id("de.solidblocks.rds.kotlin-application-conventions")
}

dependencies {
    implementation(project(":solidblocks-rds-shared"))
}

application {
    mainClass.set("de.solidblocks.rds.agent.AgentKt")
}

tasks.getByName("compileKotlin").dependsOn(":solidblocks-rds-docker:docker")