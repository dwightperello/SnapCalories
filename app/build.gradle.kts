// Module build file: HOW this app module is built. Versions live in gradle/libs.versions.toml
// (the "version catalog"); here we only reference them as libs.xxx — never raw "group:name:version".
plugins {
    alias(libs.plugins.android.application)
    // Compose compiler plugin. NO kotlin-android plugin here: AGP 9 has Kotlin built in.
    alias(libs.plugins.kotlin.compose)
    // Hilt: generates the dependency-injection code.
    alias(libs.plugins.hilt)
    // KSP = Kotlin Symbol Processing: runs annotation processors (Hilt now, Room later) at compile time.
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.snapcalories"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.snapcalories"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { compose = true } // Turn on Jetpack Compose for this module.
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Compose BOM ("bill of materials") picks matching versions for all Compose libraries,
    // which is why the compose-ui / material3 entries in the catalog have no version.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Hilt: runtime library + its code generator (ksp = runs at compile time, not shipped in the APK).
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // viewModel() support in Compose.
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // hiltViewModel() in Compose (it moved here from hilt-navigation-compose).
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // Orbit MVI: the ViewModel container + Compose helpers (collectAsState / collectSideEffect).
    implementation(libs.orbit.viewmodel)
    implementation(libs.orbit.compose)
    // Test-only (testImplementation = only for app/src/test, never in the APK):
    // orbit-test → test { expectState / expectSideEffect }; coroutines-test → runTest, test dispatchers.
    testImplementation(libs.orbit.test)
    testImplementation(libs.kotlinx.coroutines.test)
}