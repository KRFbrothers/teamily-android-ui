// Root build file.
//
// Plugins are declared here with `apply false` so the version is defined once
// for the whole build; the `:app` module applies them without repeating it.
plugins {
    // Kotlin 2.2 is supported on AGP 7.3.1 through 8.10. 8.9.3 is inside that range.
    id("com.android.application") version "8.9.3" apply false
    id("com.android.library") version "8.9.3" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
}
