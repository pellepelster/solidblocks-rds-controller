plugins {
    id("de.solidblocks.rds.kotlin-library-conventions")
    id("de.solidblocks.rds.kotlin-publish-conventions")
    id("com.palantir.docker") version "0.33.0"
}

sourceSets {
    create("assets") {
        resources {
            setSrcDirs(listOf("assets"))
            setIncludes(listOf("**/*"))
        }
    }

    main {
        kotlin {
            compileClasspath += sourceSets["assets"].resources
            runtimeClasspath += sourceSets["assets"].resources
        }
    }
}

tasks {
    withType<Jar> {
        from(sourceSets["assets"].allSource)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}


abstract class GenerateTask @Inject constructor(@get:Input val projectLayout: ProjectLayout) : DefaultTask() {

    fun generateCloudInit(target: String, files: List<String>) {
        val fileContents = files.map { file ->
            File("${projectLayout.projectDirectory}/assets/$file").readText(Charsets.UTF_8)
        }

        File("${projectLayout.projectDirectory}/assets/lib-cloud-init-generated").mkdirs()
        File("${projectLayout.projectDirectory}/assets/lib-cloud-init-generated/$target").writeText(
            fileContents.joinToString(
                "\n"
            ), Charsets.UTF_8
        )
    }

    @TaskAction
    fun generate() {

        generateCloudInit(
            "solidblocks-rds-cloud-init.sh",
            listOf(
                "lib-cloud-init/shell-script-header.sh",
                "lib-cloud-init/cloud-init-variables.sh",
                "lib/package.sh",
                "lib/curl.sh",
                "lib/bootstrap.sh",
                "lib-cloud-init/solidblocks-rds-cloud-init-body.sh"
            )
        )
    }
}
tasks.register<GenerateTask>("generate")

tasks.getByName("jar").dependsOn("generate")
tasks.getByName("docker").dependsOn("generate")

docker {
    name = "solidblocks-cloud-init-test"
    copySpec.from("$projectDir/src").into("src")
}


publishing {
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}

dependencies {
    implementation(project(":solidblocks-rds-shared"))
}
