plugins {
    id("org.springframework.boot")
}

dependencies {
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.cloud:spring-cloud-starter-sleuth")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.postgresql:postgresql")
    implementation("org.zalando:logbook-spring-boot-starter")
    implementation(kotlin("stdlib-jdk8"))

    implementation("com.momiji.api:gateway-outbound-client-starter")
    implementation("com.momiji.api:neural-text-client-starter")
    implementation("com.momiji.api:omega-momiji-api")
}
