// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    // Not applied anywhere! Declaring it only forces Gradle to use Kotlin 2.4.20 instead of the
    // older Kotlin bundled with AGP 9. Check with ./gradlew buildEnvironment ("2.2.10 -> 2.4.20").
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
