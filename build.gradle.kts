// AGP 9 has built-in Kotlin enabled by default (android.builtInKotlin=true) and
// applies the Kotlin Gradle Plugin itself, so there is no
// `org.jetbrains.kotlin.android` plugin here — declaring it as well fails the
// build with "Cannot add extension with name 'kotlin'".
//
// AGP 9.4.1 bundles KGP 2.2.10. The catalog asks for Kotlin 2.4.21 on the Kotlin
// plugin aliases below, and Gradle resolves a classpath conflict by taking the
// highest requested version for each module, so AGP's built-in Kotlin ends up
// running on 2.4.21. AGP exposes no property to set that version directly.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktor) apply false
}
