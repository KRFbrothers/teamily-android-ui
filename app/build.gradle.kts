plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

/* ============================================================================
 *  Where the backend lives — resolved ONCE, per build type.
 *
 *  The base URL is NOT a Kotlin literal. It is a generated `BuildConfig` field,
 *  so a debug build, a release build and a CI run can each point at a different
 *  service without editing source. Precedence, highest first:
 *
 *    1. -PduaApiBaseUrlDebug / -PduaApiBaseUrlRelease   (per build type)
 *    2. -PduaApiBaseUrl                                 (all build types)
 *    3. DUA_API_BASE_URL                                (environment)
 *    4. the default below                               (the live Cloud Run service)
 *
 *  Examples:
 *    ./gradlew assembleDebug -PduaApiBaseUrl=http://10.0.2.2:8080
 *    DUA_API_BASE_URL=https://staging.example ./gradlew assembleRelease
 *
 *  No secret belongs here: the base URL is a public host, and the bearer token
 *  is supplied at runtime by TokenStore, never baked into the APK.
 * ==========================================================================*/

/** The live Cloud Run service, used when nothing overrides it. */
val defaultApiBaseUrl = "https://dua-api-228305912525.us-central1.run.app"

/** The live Cloud Run engine, used when nothing overrides it. */
val defaultEngineBaseUrl = "https://dua-ai-engine-228305912525.us-central1.run.app"

/**
 * Resolves the base URL for one build type.
 *
 * @param buildTypeSuffix the per-build-type property suffix ("Debug" / "Release").
 */
fun apiBaseUrlFor(buildTypeSuffix: String): String {
    val candidates = listOf(
        providers.gradleProperty("duaApiBaseUrl$buildTypeSuffix").orNull,
        providers.gradleProperty("duaApiBaseUrl").orNull,
        providers.environmentVariable("DUA_API_BASE_URL").orNull,
    )
    return candidates.firstOrNull { !it.isNullOrBlank() }?.trim()?.trimEnd('/')
        ?: defaultApiBaseUrl
}

/**
 * Resolves the engine URL for one build type.
 *
 * Same precedence as [apiBaseUrlFor]: a per-build-type property, then the
 * shared property, then the environment, then [defaultEngineBaseUrl].
 *
 * @param buildTypeSuffix the per-build-type property suffix ("Debug" / "Release").
 */
fun engineBaseUrlFor(buildTypeSuffix: String): String {
    val candidates = listOf(
        providers.gradleProperty("duaEngineBaseUrl$buildTypeSuffix").orNull,
        providers.gradleProperty("duaEngineBaseUrl").orNull,
        providers.environmentVariable("DUA_ENGINE_BASE_URL").orNull,
    )
    return candidates.firstOrNull { !it.isNullOrBlank() }?.trim()?.trimEnd('/')
        ?: defaultEngineBaseUrl
}

android {
    namespace = "ai.duagenai.design"
    compileSdk = 37

    defaultConfig {
        applicationId = "ai.duagenai.design"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // The default for a debug build is the live service too: the app is
            // being wired to a real backend, so "debug" must not silently mean
            // "the placeholder". Override with -PduaApiBaseUrlDebug=... to point
            // at a local emulator host (http://10.0.2.2:8080) instead.
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrlFor("Debug")}\"")
            buildConfigField("String", "ENGINE_BASE_URL", "\"${engineBaseUrlFor("Debug")}\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrlFor("Release")}\"")
            buildConfigField("String", "ENGINE_BASE_URL", "\"${engineBaseUrlFor("Release")}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // Required for the generated API_BASE_URL and ENGINE_BASE_URL fields
        // above. AGP 8.x turns BuildConfig generation off by default, so without
        // this the fields do not exist and ApiConfig would not compile.
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")

    // Compose BOM pins every Compose artifact to one compatible set.
    // 2026.04.01 ships Material3 1.4.0 (public APIs only; expressive APIs stay internal).
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Create Agent's shape morph (Morph + RoundedPolygon).
    implementation("androidx.graphics:graphics-shapes:1.0.1")

    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.navigation:navigation-testing:2.10.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
