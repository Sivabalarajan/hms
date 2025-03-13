plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.android.hms"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.android.hms"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ui.auth)
    implementation(libs.google.firebase.auth)
    implementation(libs.google.firebase.firestore)
    implementation(libs.com.google.firebase.firebase.firestore)
    implementation(libs.google.firebase.messaging)
    implementation(libs.com.google.firebase.firebase.auth)
    // implementation (libs.firebase.ui.auth.v802)
    // implementation(libs.firebase.ui.auth.v720)

    // implementation(libs.firebase.messaging)

    // implementation(platform(libs.firebase.bom.v3370))
    // implementation(libs.firebase.ui.auth.v802)

    implementation (libs.fuel)  // Fuel HTTP Client
    implementation (libs.fuel.coroutines)  // For coroutine support

    implementation(libs.material)

    implementation(libs.poi)
    implementation(libs.poi.ooxml)
    implementation(libs.google.auth.library.oauth2.http)

}