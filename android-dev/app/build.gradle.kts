import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Create a properties object and load the local.properties file
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.app"
    compileSdk = 36 // Updated from 34 to 36

    defaultConfig {
        applicationId = "com.example.app"
        minSdk = 24
        targetSdk = 36 // Updated from 34 to 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Make the API key available in the BuildConfig class
        buildConfigField("String", "HUGGING_FACE_API_KEY", "\"${localProperties.getProperty("HUGGING_FACE_API_KEY")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // For making network calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // For converting Java objects to and from JSON
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}