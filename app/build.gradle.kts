plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)   // v2.0 — per ADR-001
    alias(libs.plugins.ksp)              // v2.0 — Room/KSP per ADR-003
}

val releaseStoreFile = providers.gradleProperty("SEACHEM_RELEASE_STORE_FILE")
    .orElse(providers.environmentVariable("SEACHEM_RELEASE_STORE_FILE"))
val releaseStorePassword = providers.gradleProperty("SEACHEM_RELEASE_STORE_PASSWORD")
    .orElse(providers.environmentVariable("SEACHEM_RELEASE_STORE_PASSWORD"))
val releaseKeyAlias = providers.gradleProperty("SEACHEM_RELEASE_KEY_ALIAS")
    .orElse(providers.environmentVariable("SEACHEM_RELEASE_KEY_ALIAS"))
val releaseKeyPassword = providers.gradleProperty("SEACHEM_RELEASE_KEY_PASSWORD")
    .orElse(providers.environmentVariable("SEACHEM_RELEASE_KEY_PASSWORD"))
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { it.isPresent }

android {
    namespace = "com.example.seachem_dosing"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.seachem_dosing"
        minSdk = 33   // v2.0 — Android 13+ per ADR-006 (owner decision); ~68.9% device coverage (DEEP_RESEARCH_REPORT WS1)
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
            isJniDebuggable = false
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val appName = "SeachemDosing"
            val versionName = variant.versionName
            val buildType = variant.buildType.name
            output.outputFileName = "${appName}-v${versionName}-${buildType}.apk"
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true                   // v2.0 — per ADR-001
    }

    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"  // v2.0 — required by Compose runtime
        }
    }

    compileOptions {
        // v2.0 — bumped from JDK 11 → 17 (required by Koin 4.x, AGP 8.x toolchain).
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

// v2.0 — Room schema export location (per ADR-003)
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    // ===== v1.0 Core (XML stack — kept active during Compose migration) =====
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Material Design 3 (XML)
    implementation("com.google.android.material:material:1.11.0")

    // Constraint Layout (XML)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Navigation (Fragment-based — XML stack)
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Lifecycle & ViewModel (LiveData kept until Phase 4.7 StateFlow mig)
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

    // Preferences DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // ===== v2.0 — Compose stack (per ADR-001) =====
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime.livedata)   // observeAsState: LiveData -> Compose bridge during migration
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // ===== v2.0 — Koin DI (per ADR-002) =====
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)

    // ===== v2.0 — Room persistence (per ADR-003) =====
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.androidx.room.testing)

    // ===== Testing =====
    testImplementation(libs.junit)
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.json:json:20240303")
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)   // MigrationTestHelper (instrumented)
}
