import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}


android {
    namespace = "com.salmane.quran"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.salmane.quran"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "2.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
        }
    }

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
    }

    signingConfigs {
        create("release") {
            val keystorePath =
                System.getenv("KEYSTORE_PATH")
                    ?: localProperties["KEYSTORE_PATH"] as String?
                    ?: "release.keystore"

            val keystorePassword =
                System.getenv("KEYSTORE_PASSWORD")
                    ?: localProperties["KEYSTORE_PASSWORD"] as String?

            val keyAliasValue =
                System.getenv("KEY_ALIAS")
                    ?: localProperties["KEY_ALIAS"] as String?

            val keyPasswordValue =
                System.getenv("KEY_PASSWORD")
                    ?: localProperties["KEY_PASSWORD"] as String?

            storeFile = file(keystorePath)
            storePassword = keystorePassword
            keyAlias = keyAliasValue
            keyPassword = keyPasswordValue
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            ndk {
                debugSymbolLevel = "FULL"
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("com.google.code.gson:gson:2.10.1")
}
