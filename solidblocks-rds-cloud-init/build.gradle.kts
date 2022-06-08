plugins {
    id("de.solidblocks.rds.kotlin-library-conventions")
    id("de.solidblocks.rds.kotlin-publish-conventions")
    id("com.palantir.docker") version "0.33.0"
}

dependencies {
    implementation(project(":solidblocks-rds-shared"))
    implementation("org.reflections:reflections:0.10.2")
}

sourceSets {
    create("assets") {
        resources {
            setSrcDirs(listOf("assets"))
            setIncludes(listOf("**/*"))
        }
    }
}

abstract class GenerateTask @Inject constructor(@get:Input val projectLayout: ProjectLayout) : DefaultTask() {

    fun generateCloudInit(target: String, files: List<String>) {
        val fileContents = files.map { file ->
            File("${projectLayout.projectDirectory}/assets/$file").readText(Charsets.UTF_8)
        }

        File("${projectLayout.projectDirectory}/src/main/resources/templates-generated").mkdirs()
        File("${projectLayout.projectDirectory}/src/main/resources/templates-generated/$target").writeText(
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
tasks.getByName("jar").dependsOn("assetsJar")
tasks.getByName("jar").dependsOn("generate")
tasks.getByName("docker").dependsOn("generate")

docker {
    name = "solidblocks-cloud-init-test"
    copySpec.from("$projectDir/src").into("src")
}

val assetsJarTask = tasks.register<Jar>("assetsJar") {
    from(sourceSets["assets"].output)
    this.archiveClassifier.set("assets")
}.get()

publishing {
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
            artifact(assetsJarTask)
        }
    }
}
