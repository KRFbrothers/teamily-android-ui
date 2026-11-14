# Keep the Compose runtime's reflective entry points.
-keep class androidx.compose.runtime.** { *; }

# org.json is part of the platform, but keep the model classes readable in
# stack traces when minification is enabled for a release build.
-keepclassmembers class ai.duagenai.design.** {
    <fields>;
}
