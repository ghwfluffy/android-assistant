plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun configValue(name: String, default: String): String {
    val propertyValue = providers.gradleProperty(name).orNull
    val envValue = providers.environmentVariable(name).orNull
    return propertyValue ?: envValue ?: default
}

fun quoted(value: String): String {
    val escaped = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
    return "\"$escaped\""
}

android {
    namespace = "com.ghwfluffy.assistantwrapper"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.ghwfluffy.assistantwrapper"
        minSdk = 26
        targetSdk = 35
        versionCode = configValue("ANDROID_VERSION_CODE", "1").toInt()
        versionName = configValue("ANDROID_VERSION_NAME", "0.1.0")

        buildConfigField("String", "SITE_ORIGIN", quoted(configValue("ANDROID_SITE_ORIGIN", "https://example.invalid")))
        buildConfigField("String", "AUTH_BASE_PATH", quoted(configValue("ANDROID_AUTH_BASE_PATH", "/auth")))
        buildConfigField("String", "AGENT_BASE_PATH", quoted(configValue("ANDROID_AGENT_BASE_PATH", "/agent")))
        buildConfigField("String", "DEFAULT_START_PATH", quoted(configValue("ANDROID_DEFAULT_START_PATH", "/auth")))
        buildConfigField("String", "APP_SHORTCUTS_JSON", quoted(configValue("ANDROID_APP_SHORTCUTS_JSON", """[{"label":"Directory","path":"/auth"}]""")))
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
