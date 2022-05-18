plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.4.0"
}

group = "cxs"
version = "1.4.1"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    version.set("2021.2")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    patchPluginXml {
        sinceBuild.set("212")
        untilBuild.set("222.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

dependencies {
    implementation("org.apache.httpcomponents:httpclient:4.5.5")
    implementation("javax.servlet:javax.servlet-api:3.1.0")
    implementation("redis.clients:jedis:2.9.0")
    //testImplementation("com.jetbrains.intellij.copyright:copyright:${ideVersion}")
}
