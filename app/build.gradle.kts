plugins {
    alias(libs.plugins.android.application)

    // Firebase / Google Services
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.appkiora"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.appkiora"

        minSdk = 24
        targetSdk = 37

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    // Retrofit e comunicação com a API Kiora.
    implementation(
        "com.squareup.retrofit2:converter-gson:2.11.0"
    )
    implementation(
        "com.squareup.retrofit2:retrofit:2.11.0"
    )

    // Corrotinas Kotlin.
    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0"
    )

    // Imagens.
    implementation("io.coil-kt:coil:2.7.0")

    // Componentes Android existentes.
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Firebase: versões compatíveis gerenciadas pelo BoM.
    implementation(
        platform("com.google.firebase:firebase-bom:35.2.0")
    )

    // Recebimento de notificações push.
    implementation(
        "com.google.firebase:firebase-messaging"
    )

    // Testes existentes.
    testImplementation(libs.junit)
    androidTestImplementation(
        libs.androidx.espresso.core
    )
    androidTestImplementation(
        libs.androidx.junit
    )
}