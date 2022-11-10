plugins {
    `maven-publish`
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("s3://solidblocks/")
            credentials(AwsCredentials::class) {
                accessKey = System.getenv("R2_ACCESS_KEY")
                secretKey = System.getenv("R2_SECRET_ACCESS_KEY")
            }
        }
    }
}

