plugins {
    alias(libs.plugins.agp) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint.plugin) apply false
}

apply {
    from("gradle/translators.gradle.kts")
}
