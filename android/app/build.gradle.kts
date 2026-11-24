plugins {
    id("com.android.application")
}

android {
    namespace = "com.rhf.port"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rhf.port"
        minSdk = 29
        targetSdk = 35
        versionCode = System.getenv("RHF_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("RHF_VERSION_NAME")?.takeIf { it.isNotEmpty() } ?: "0.1.0"
        ndk {
            // libmain.so llega ya compilado desde el flujo de CI (CMake + NDK 27). No se compila aquí.
            abiFilters += listOf("arm64-v8a")
        }
    }

    // Firma: si hay keystore (secretos del CI), se usa; si no, la clave de depuración.
    val keystorePath = System.getenv("RHF_KEYSTORE_PATH")?.takeIf { it.isNotEmpty() }
    signingConfigs {
        if (keystorePath != null) {
            create("rhf") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("RHF_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RHF_KEY_ALIAS")
                keyPassword = System.getenv("RHF_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (keystorePath != null) {
                signingConfigs.getByName("rhf")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
