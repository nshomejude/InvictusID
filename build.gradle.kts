// Root build file. Subprojects apply plugins from the version catalog directly;
// the root declares none, so a sandbox without access to Google's maven (which
// serves the Android Gradle Plugin) can still build :shared and :backend.
//
// See gradle/libs.versions.toml for all plugin and dependency versions.
