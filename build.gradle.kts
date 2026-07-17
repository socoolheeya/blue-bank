plugins {
    kotlin("jvm") version "2.3.21" apply false
    kotlin("plugin.spring") version "2.3.21" apply false
    kotlin("plugin.jpa") version "2.3.21" apply false
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

extra["springCloudVersion"] = "2025.1.2"

allprojects {
    group = "com.socoolheeya.bluebank"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}