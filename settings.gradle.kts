rootProject.name = "childtrace"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// Pure-Kotlin core: domain model + on-device risk engine. No Android dependency,
// so it builds and tests on any JVM (CI, laptop) without the Android SDK.
include(":shared")

// Kotlin/Ktor backend: pairing, flagged-alert sync, consent records.
include(":backend")

// Android apps. These require the Android SDK + the Android Gradle Plugin (served
// from Google's maven). They are wired into the build only when an SDK is present,
// so a CI box or sandbox without one still builds :shared and :backend cleanly.
// On a developer machine with Android Studio the SDK is detected automatically.
val androidSdkAvailable =
    System.getenv("ANDROID_HOME") != null ||
        System.getenv("ANDROID_SDK_ROOT") != null ||
        file("local.properties").exists()

if (androidSdkAvailable) {
    include(":child-app")
    include(":parent-app")
} else {
    logger.lifecycle(
        "ChildTrace: no Android SDK detected — building :shared and :backend only. " +
            "Set ANDROID_HOME (or add local.properties) to include :child-app and :parent-app."
    )
}
