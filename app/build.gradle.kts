plugins { id("com.android.application"); id("org.jetbrains.kotlin.plugin.compose") }

android { namespace = "es.electrohobby3d.armor"; compileSdk = 37
    defaultConfig { applicationId = "es.electrohobby3d.armor"; minSdk = 26; targetSdk = 37; versionCode = 26; versionName = "0.4.4" }
    buildFeatures { compose = true; buildConfig = true }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    // The Android stubs of org.json do nothing in local tests; the real library lets the event parser be tested.
    testImplementation("org.json:json:20250517")
}
