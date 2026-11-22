// Root build file.
//
// Plugins are declared here with `apply false` so the version is defined once
// for the whole build; the `:app` module applies them without repeating it.
plugins {
    id("com.android.application") version "9.2.0" apply false
    id("com.android.library") version "9.2.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
}
