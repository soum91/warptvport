plugins { id("com.android.application") }

android {
    namespace = "com.soum91.tunertv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.soum91.tunertv"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Release signing is configured with local properties / CI secrets, never in git.
            val keystore = providers.gradleProperty("TUNERTV_KEYSTORE_PATH").orNull
            if (keystore != null) {
                signingConfig = signingConfigs.create("personal") {
                    storeFile = file(keystore)
                    storePassword = providers.gradleProperty("TUNERTV_STORE_PASSWORD").get()
                    keyAlias = providers.gradleProperty("TUNERTV_KEY_ALIAS").get()
                    keyPassword = providers.gradleProperty("TUNERTV_KEY_PASSWORD").get()
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.11.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    testImplementation("junit:junit:4.13.2")
}

