plugins {
    id("de.solidblocks.rds.kotlin-library-conventions")
}

dependencies {
    api("com.squareup.okhttp3:okhttp:4.9.3")

    implementation("org.freemarker:freemarker:2.3.31")
    implementation("org.apache.commons:commons-text:1.9")
    implementation("com.jcabi:jcabi-manifests:1.1")
}
