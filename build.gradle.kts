plugins {
    kotlin("jvm") version "1.7.21"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "net.azisaba"
version = "1.0.0-SNAPSHOT"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("dev.kord:kord-core:0.8.0-M17")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.0.6")
    implementation("org.yaml:snakeyaml:1.33")
}

tasks {
    shadowJar {
        manifest {
            attributes(
                "Main-Class" to "net.azisaba.azisabareportbot.MainKt",
            )
        }
        archiveFileName.set("AzisabaReportBot.jar")
    }
}
