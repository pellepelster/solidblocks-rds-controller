plugins {
    id("de.solidblocks.rds.kotlin-application-conventions")
}

dependencies {
    implementation(project(":solidblocks-rds-shared"))
    implementation("com.github.ajalt.clikt:clikt:3.4.1")
}

application {
    mainClass.set("de.solidblocks.rds.agent.AgentKt")
}

tasks.getByName("compileKotlin").dependsOn(":solidblocks-rds-docker:docker")