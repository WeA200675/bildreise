plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

fun configured(name: String, environment: String): String =
    providers.gradleProperty(name).orElse(providers.environmentVariable(environment)).orElse("").get().trim()

fun asBuildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val modelBundleUrl = configured("bildreise.modelBundleUrl", "BILDREISE_MODEL_BUNDLE_URL")
val modelBundleSha256 = configured("bildreise.modelBundleSha256", "BILDREISE_MODEL_BUNDLE_SHA256")
require((modelBundleUrl.isBlank() && modelBundleSha256.isBlank()) ||
    (modelBundleUrl.isNotBlank() && modelBundleSha256.matches(Regex("[0-9a-fA-F]{64}")))) {
    "Setze bildreise.modelBundleUrl und bildreise.modelBundleSha256 gemeinsam; der Hash muss exakt 64 Hex-Zeichen enthalten."
}

android { namespace="app.bildreise"; compileSdk=35
    defaultConfig { applicationId="app.bildreise"; minSdk=26; targetSdk=35; versionCode=1; versionName="0.1.0"
        buildConfigField("String", "MODEL_URL", "\"\"")
        buildConfigField("String", "MODEL_SHA256", "\"\"")
        buildConfigField("String", "MODEL_BUNDLE_URL", asBuildConfigString(modelBundleUrl))
        buildConfigField("String", "MODEL_BUNDLE_SHA256", asBuildConfigString(modelBundleSha256.lowercase()))
        buildConfigField("String", "MODEL_BACKEND", "\"mobile-diffusion\"")
    }
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    // Replace with the current LiteRT Play-services artifact when enabling the model.
    implementation("com.google.android.gms:play-services-tflite-java:16.5.0")
    implementation("com.google.android.gms:play-services-tflite-gpu:16.5.0")
    implementation("com.google.mediapipe:tasks-vision-image-generator:latest.release")
}
