plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.mclone.stbemu"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mclone.stbemu"
        minSdk = 21
        targetSdk = 28          // bewusst 28: targetSdk>=29 erzwingt scoped storage und
                                // Hintergrund-Startrestriktionen, die der STB-Emulator
                                // für USB-/PVR-/Boot-Autostart braucht.
        versionCode = 1
        versionName = "1.0.0"
        resourceConfigurations += listOf("en", "de", "ru", "uk")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*"
        )
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.material)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.jackson.databind)
    // SQLCipher optional - community build not in mavenCentral
    // implementation("net.zetetic:android-database-sqlcipher:4.5.4")

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // Optional: IJK/MVP-Backends werden als separate Module ergänzt,
    // siehe docs/PROJECT_PLAN.md, Meilenstein 4.
}
