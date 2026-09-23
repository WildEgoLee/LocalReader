// JVM-only so locator and index tests run without the Android SDK.
// The app compiles these sources through its source set, not as a subproject.
plugins {
    kotlin("jvm") version "2.0.21"
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    testImplementation(kotlin("test"))
}
