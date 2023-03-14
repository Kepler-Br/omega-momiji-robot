plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.postgresql:postgresql")
    implementation(kotlin("stdlib-jdk8"))

    val gatewayApiVersion: String by project

    implementation("com.momiji.gateway:omega-momiji-messenger-gateway-api:$gatewayApiVersion")
    implementation(project(":${rootProject.name}-api"))
}
