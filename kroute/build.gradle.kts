plugins {
    kotlin("jvm")
    `maven-publish`
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:1.6.10-1.0.2")
    implementation("com.squareup:kotlinpoet:1.10.2")
    implementation("com.squareup:kotlinpoet-ksp:1.10.2")
}


afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = "moe.tlaster"
                artifactId = "kroute"
                version = "1.0.0"

                from(components["java"])
            }
        }
    }
}