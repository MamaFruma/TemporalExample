repositories {
    mavenCentral()
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11

plugins {
    application
    java
    id("idea")
    id("org.springframework.boot") version "2.4.5"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

dependencies {
    // Application dependencies
    implementation("io.temporal:temporal-sdk:1.0.7")
    implementation("org.springframework.boot:spring-boot-dependencies:2.4.5")
    implementation("org.springframework.boot:spring-boot-starter-webflux:2.4.5")
    implementation("org.springframework.boot:spring-boot-starter-actuator:2.4.5")
    implementation("org.projectlombok:lombok:1.18.20")
    implementation("io.micrometer:micrometer-registry-prometheus:1.6.3")
    annotationProcessor("org.projectlombok:lombok:1.18.20")

    // Testing dependencies
    testImplementation("junit:junit:4.13")
    testImplementation("org.mockito:mockito-all:1.10.19")
}



application {
    mainClass.set("example.temporal.TemporalApplication")
}