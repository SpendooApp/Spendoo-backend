plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "1.9.22"
}

repositories {
    mavenCentral()
}


dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    compileOnly("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")
    runtimeOnly("org.postgresql:postgresql")
    implementation(projects.events)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("com.google.truth:truth:1.4.4")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


tasks.withType<Test> {
    useJUnitPlatform()
}