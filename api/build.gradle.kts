plugins {
    id("java-library")
    id("maven-publish")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-web")

    api("com.fasterxml.jackson.core:jackson-annotations")
    api("org.springframework.data:spring-data-commons")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
