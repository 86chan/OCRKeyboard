import java.util.Properties
import java.io.FileInputStream
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
}

/**
 * 署名情報を保持するローカルファイルの参照
 */
val keystorePropertiesFile: File = rootProject.file("local.properties")

/**
 * 読み込まれた署名設定のプロパティ
 */
val keystoreProperties: Properties = Properties()

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.haru.ocrkeyboard"
    compileSdk = 36

    signingConfigs {
        create("release") {
            storeFile = keystoreProperties["signing.storeFile"]?.let { file(it) }
            storePassword = keystoreProperties["signing.storePassword"] as String?
            keyAlias = keystoreProperties["signing.keyAlias"] as String?
            keyPassword = keystoreProperties["signing.keyPassword"] as String?
        }
    }

    defaultConfig {
        applicationId = "com.haru.ocrkeyboard"
        minSdk = 36
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.accompanist.permissions)
    implementation(libs.play.services.location)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.core)
    implementation(libs.logging.interceptor)
    implementation(libs.okhttp)
    implementation(libs.moshi.kotlin)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.material)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.text.recognition.japanese)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.runner)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.mockito.core)
    "ksp"(libs.moshi.kotlin.codegen)
    "ksp"(libs.androidx.room.compiler)
}
