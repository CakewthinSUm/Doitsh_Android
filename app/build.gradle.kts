// Imported rather than written inline: inside this script `java` resolves to the
// project's Java extension, so `java.util.Properties()` does not compile.
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // No org.jetbrains.kotlin.android here: AGP 9 applies Kotlin itself.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Release signing. The keystore lives outside the repository (`~/.android` by default)
// and its credentials are read from, in order of precedence:
//
//   1. Gradle properties — `-Pdoitsh.storePassword=...` — which is what CI would use;
//   2. `doitsh-release.properties` in the repository root, gitignored, for a workstation;
//   3. environment variables `DOITSH_STORE_PASSWORD` / `DOITSH_KEY_PASSWORD`.
//
// Nothing secret is committed: when the passwords are absent the release build is left
// unsigned rather than failing the whole `assemble`, so a fresh clone still builds and
// `:app:assembleRelease` produces `app-release-unsigned.apk`.
//
// `storeFile` may be absolute, or `~/...`, or relative to the repository root.
val localSigning = Properties().apply {
    val file = rootProject.file("doitsh-release.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(key: String): String? =
    (findProperty(key) as String?)
        ?: localSigning.getProperty(key)
        ?: System.getenv(key.replace('.', '_').uppercase())

val releaseStoreFile = file(
    signingValue("doitsh.storeFile")
        ?: "${System.getProperty("user.home")}/.android/doitsh-release.jks"
)
val releaseStorePassword = signingValue("doitsh.storePassword")
val releaseKeyPassword = signingValue("doitsh.keyPassword")
val releaseKeyAlias = signingValue("doitsh.keyAlias") ?: "doitsh"

android {
    namespace = "com.doitsh.app"
    // compileSdk is 37, not the 36 the version table asks for: every library in that
    // same table (Compose 1.12.1, core-ktx 1.19.1, lifecycle 2.11.0, navigation3 1.2.0,
    // Coil 3.6.3, OkHttp 5.5.0) ships AAR metadata declaring
    // minCompileSdk=37, and AGP 9 fails the build outright on a mismatch. targetSdk
    // stays at 36 — compiling against newer APIs and opting in to new runtime
    // behaviour are separate choices, and 36 is the one the table specified.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.doitsh.app"
        minSdk = 34
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val keystoreUsable = releaseStoreFile.exists() &&
            releaseStorePassword != null && releaseKeyPassword != null
        if (keystoreUsable) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Only wire the config up when the keystore was actually found, so a clone
            // without one still builds (and produces an unsigned APK).
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // SettingsScreen reads BuildConfig.VERSION_NAME for the About section.
        buildConfig = true
    }
}

// Was `android.kotlinOptions {}` before AGP 9. Built-in Kotlin moves the Kotlin
// configuration to a top-level `kotlin` extension, and inherits jvmTarget from
// android.compileOptions.targetCompatibility above.
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xopt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "-Xopt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            // FlowRow, for the sheet's chip rows. Chips are sized to their own labels
            // rather than to equal weights, so a long label ("Tomorrow", and worse in
            // languages that are longer than English) wraps the row instead of being
            // truncated mid-word.
            "-Xopt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi"
        )
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM. Note it pins material3 to 1.4.0, which lacks the Expressive
    // FAB-menu APIs used in DoitshApp.kt — so material3 is declared with an
    // explicit version below, which takes precedence over the BOM.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Adaptive layouts
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)

    // Navigation 3 (replaces androidx.navigation:navigation-compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Ktor Client
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Dependency Injection - Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // Replaces androidx.hilt:hilt-navigation-compose, which is deprecated and
    // would pull Navigation 2 back in alongside Navigation 3.
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // Serialization
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // DataStore / WorkManager / Paging / Collection
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.collection.ktx)

    // Images / network stack
    implementation(libs.coil.compose)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Baseline profiles
    implementation(libs.androidx.profileinstaller)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
