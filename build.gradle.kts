import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test

plugins {
    kotlin("jvm") version "2.3.21" apply false
    kotlin("plugin.spring") version "2.3.21" apply false
    kotlin("plugin.jpa") version "2.3.21" apply false
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("de.infix.testBalloon") version "1.0.0-K2.3.20" apply false
}

val testBalloonPlatform by configurations.creating

dependencies {
    add(testBalloonPlatform.name, "org.junit.platform:junit-platform-commons:1.13.4")
    add(testBalloonPlatform.name, "org.junit.platform:junit-platform-engine:1.13.4")
    add(testBalloonPlatform.name, "org.junit.platform:junit-platform-launcher:1.13.4")
}

extra["springCloudVersion"] = "2025.1.2"

allprojects {
    group = "com.socoolheeya.bluebank"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    configurations.configureEach {
        resolutionStrategy.force(
            "org.junit:junit-bom:5.13.4",
            "org.junit.platform:junit-platform-commons:1.13.4",
            "org.junit.platform:junit-platform-engine:1.13.4",
            "org.junit.platform:junit-platform-launcher:1.13.4"
        )
    }
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        val sourceSets = extensions.getByType<SourceSetContainer>()
        tasks.register<Test>("testBalloon") {
            description = "Runs TestBalloon tests with an isolated JUnit Platform 1.13 runtime"
            group = "verification"
            testClassesDirs = sourceSets["test"].output.classesDirs
            val bootRuntime = configurations.named("testRuntimeClasspath")
            classpath = files(
                sourceSets["main"].output,
                sourceSets["test"].output,
                bootRuntime.map { configuration ->
                    configuration.filterNot { file ->
                        file.name.startsWith("junit-platform-") ||
                            file.name.startsWith("junit-jupiter-") ||
                            file.name.startsWith("junit-bom-")
                    }
                },
                rootProject.configurations[testBalloonPlatform.name]
            )
            useJUnitPlatform()
        }
    }
}
