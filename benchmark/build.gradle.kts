plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.richardittou.qrcodenow.benchmark"
    compileSdk = 37

    defaultConfig {
        minSdk = 33
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.junit)
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.androidx.uiautomator)
}
