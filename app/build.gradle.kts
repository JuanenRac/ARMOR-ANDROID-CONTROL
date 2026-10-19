plugins { id("com.android.application"); id("org.jetbrains.kotlin.plugin.compose") }

// Push through Firebase is the installation's own: the build reads app/google-services.json (the file Firebase's console gives, which is NOT kept in the repository) and turns what the app needs into
// build constants; with no such file the constants are empty and the app has no push at all (it still watches the server itself). See docs/PUSH_NOTIFICATIONS.md.
val firebase: Map<String, String> = run {
    val file = file("google-services.json")
    if (!file.exists()) return@run emptyMap<String, String>()
    @Suppress("UNCHECKED_CAST")
    val root = groovy.json.JsonSlurper().parse(file) as Map<String, Any?>
    val info = root["project_info"] as Map<String, Any?>
    val clients = (root["client"] as List<Map<String, Any?>>).filter { (((it["client_info"] as Map<String, Any?>)["android_client_info"]) as Map<String, Any?>)["package_name"] == "es.electrohobby3d.armor" }
    check(clients.isNotEmpty()) { "app/google-services.json has no client for es.electrohobby3d.armor: register the Android app with exactly that package name in the Firebase console" }
    val client = clients.first()
    mapOf("project" to info["project_id"].toString(), "sender" to info["project_number"].toString(), "app" to ((client["client_info"] as Map<String, Any?>)["mobilesdk_app_id"]).toString(),
        "key" to (((client["api_key"] as List<Map<String, Any?>>).first())["current_key"]).toString())
}

android { namespace = "es.electrohobby3d.armor"; compileSdk = 37
    defaultConfig { applicationId = "es.electrohobby3d.armor"; minSdk = 26; targetSdk = 37; versionCode = 54; versionName = "0.7.2"
        buildConfigField("String", "FCM_PROJECT_ID", "\"${firebase["project"] ?: ""}\"")
        buildConfigField("String", "FCM_SENDER_ID", "\"${firebase["sender"] ?: ""}\"")
        buildConfigField("String", "FCM_APP_ID", "\"${firebase["app"] ?: ""}\"")
        buildConfigField("String", "FCM_API_KEY", "\"${firebase["key"] ?: ""}\"")
    }
    buildFeatures { compose = true; buildConfig = true }
}
// The version lives in two places the Android tools need (here, and the manifest every other tool reads); a build stops when they differ,
// so a release can never carry a number the project's own manifest does not say.
afterEvaluate {
    val manifestVersion = (groovy.json.JsonSlurper().parse(rootProject.file("armor.project.json")) as Map<*, *>)["version"]
    val appVersion = android.defaultConfig.versionName
    check(manifestVersion == appVersion) { "armor.project.json says $manifestVersion but versionName is $appVersion: change them together (tools/armor_project_tool.py bump)" }
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
    // Push messages from the installation's own Firebase project (see PushService); the app starts Firebase itself from the build constants, so no Google Services plugin is needed.
    implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    // The Android stubs of org.json do nothing in local tests; the real library lets the event parser be tested.
    testImplementation("org.json:json:20250517")
    // The state holders run their work on the main dispatcher, which a local test replaces with a test one.
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
