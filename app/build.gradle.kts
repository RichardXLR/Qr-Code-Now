@file:Suppress("DEPRECATION")

import java.util.Properties

val privateSigningFile = rootProject.file("keystore.properties")
val privateSigningProperties = Properties().apply {
    if (privateSigningFile.isFile) privateSigningFile.inputStream().use(::load)
}
val releaseStorePath = providers.gradleProperty("RELEASE_STORE_FILE").orNull
    ?: privateSigningProperties.getProperty("RELEASE_STORE_FILE")
val releaseStorePassword = providers.gradleProperty("RELEASE_STORE_PASSWORD").orNull
    ?: privateSigningProperties.getProperty("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS").orNull
    ?: privateSigningProperties.getProperty("RELEASE_KEY_ALIAS")
val releaseKeyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD").orNull
    ?: privateSigningProperties.getProperty("RELEASE_KEY_PASSWORD")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.richardittou.qrcodenow"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.richardittou.qrcodenow"
        minSdk = 33
        targetSdk = 37
        versionCode = 2
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (listOf(releaseStorePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { it != null }) {
            create("release") {
                storeFile = file(requireNotNull(releaseStorePath))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        localeFilters += listOf("pt", "pt-rBR")
    }

    packaging.resources.excludes += setOf(
        "/META-INF/{AL2.0,LGPL2.1}",
        "META-INF/DEPENDENCIES",
        "META-INF/LICENSE*",
        "META-INF/NOTICE*"
    )

    testOptions.unitTests.isIncludeAndroidResources = true
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.sqlite.framework)
    implementation(libs.androidx.datastore)
    implementation(libs.zxing.core)
    implementation(libs.coroutines.android)
    implementation(libs.androidx.profileinstaller)
    "baselineProfile"(project(":benchmark"))

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

baselineProfile {
    automaticGenerationDuringBuild = false
}
