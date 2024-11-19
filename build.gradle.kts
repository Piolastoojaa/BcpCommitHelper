plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("idea")
}

group = "com.duberlyguarnizo"
version = "1.0.6-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        bundledPlugin("com.intellij.java")
        create("IC", "2023.1.5")
        instrumentationTools()
    }
}

intellijPlatform {
    buildSearchableOptions = false
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "231"
            untilBuild = "243.*"
        }
    }
    publishing {
        token = System.getenv("PUBLISH_TOKEN")
    }
    signing {
        privateKey = System.getenv("PRIVATE_KEY")
        password = System.getenv("PRIVATE_KEY_PASSWORD")
        certificateChain = System.getenv("CERTIFICATE_CHAIN")
    }

}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    idea {
        module {
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }


}
