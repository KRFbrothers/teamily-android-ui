// Root build file.
//
// Plugins are declared here with `apply false` so the version is defined once
// for the whole build; the `:app` module applies them without repeating it.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
