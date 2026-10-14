plugins {
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

application {
    mainClass.set("com.doitsh.server.ApplicationKt")
}

dependencies {
    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.serialization.kotlinx.json.server)

    // Exposed ORM
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)

    // SQLite driver
    implementation(libs.sqlite.jdbc)
    // PostgreSQL driver (optional, user can switch)
    implementation(libs.postgresql)

    // HikariCP connection pool
    implementation(libs.hikaricp)

    // Logging
    implementation(libs.logback.classic)

    // BCrypt for password hashing
    implementation(libs.jbcrypt)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
}
