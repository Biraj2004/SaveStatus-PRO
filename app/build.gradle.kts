plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp)
}

import java.util.Properties

android {
    namespace = "com.savestatus.pro"
    compileSdk = 34

    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties().apply {
        if (keystorePropsFile.exists()) {
            keystorePropsFile.inputStream().use { load(it) }
        }
    }

    defaultConfig {
        applicationId = "com.savestatus.pro"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProps.getProperty("storeFile")
            val storePwd = keystoreProps.getProperty("storePassword")
            val alias = keystoreProps.getProperty("keyAlias")
            val keyPwd = keystoreProps.getProperty("keyPassword")

            if (!storeFilePath.isNullOrBlank() && !storePwd.isNullOrBlank() && !alias.isNullOrBlank() && !keyPwd.isNullOrBlank()) {
                storeFile = rootProject.file(storeFilePath)
                storePassword = storePwd
                keyAlias = alias
                keyPassword = keyPwd
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isCrunchPngs = false

            val hasSigningConfig = !keystoreProps.getProperty("storeFile").isNullOrBlank() &&
                !keystoreProps.getProperty("storePassword").isNullOrBlank() &&
                !keystoreProps.getProperty("keyAlias").isNullOrBlank() &&
                !keystoreProps.getProperty("keyPassword").isNullOrBlank()

            if (!hasSigningConfig) {
                throw GradleException(
                    "Missing release signing config. Create keystore.properties in project root with storeFile, storePassword, keyAlias, keyPassword."
                )
            }

            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = false
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.glide)
    ksp(libs.glide.compiler)
    implementation(libs.exoplayer.core)
    implementation(libs.exoplayer.ui)
    implementation(libs.coroutines.android)
    implementation(libs.photoview)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// ── Export APK task ────────────────────────────────────────────
tasks.register<Copy>("exportApk") {
    dependsOn("assembleRelease")
    val versionName = android.defaultConfig.versionName ?: "release"
    from(layout.buildDirectory.dir("outputs/apk/release"))
    include("app-release.apk")
    into(rootProject.layout.projectDirectory.dir("APK_Export"))
    rename("app-release.apk", "SaveStatus_PRO_v${versionName}.apk")
    doLast {
        println("✓ APK exported to: APK_Export/SaveStatus_PRO_v${versionName}.apk")
    }
}

