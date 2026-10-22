import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// 비밀 값은 저장소에 두지 않는다. 로컬은 local.properties, CI는 GitHub Secrets → 환경변수로 주입.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(envName: String, propName: String): String? =
    System.getenv(envName)?.takeIf { it.isNotBlank() } ?: localProps.getProperty(propName)?.takeIf { it.isNotBlank() }

val appVersionName = System.getenv("VERSION_NAME")?.removePrefix("v") ?: "1.0.0"
val appVersionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1

android {
    namespace = "com.hdlee73.everylaw"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hdlee73.everylaw"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        // 국가법령정보 공동활용 OC. 비어 있으면 앱은 데모 모드로 동작하고, 설정 화면에서 직접 입력할 수 있다.
        buildConfigField("String", "LAW_OC", "\"${secret("LAW_OC", "law.oc") ?: ""}\"")
        buildConfigField("String", "UPDATE_REPO", "\"hdlee73/Everylaw\"")
        buildConfigField("String", "AUTHOR_NAME", "\"이현덕\"")
        buildConfigField("String", "AUTHOR_EMAIL", "\"hdlee73@gmail.com\"")
    }

    signingConfigs {
        create("release") {
            val storePath = secret("KEYSTORE_FILE", "release.storeFile")
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = secret("KEYSTORE_PASSWORD", "release.storePassword")
                keyAlias = secret("KEY_ALIAS", "release.keyAlias")
                keyPassword = secret("KEY_PASSWORD", "release.keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 키가 주입된 경우(공식 릴리스 워크플로)에만 서명한다. 포크·로컬 빌드는 서명되지 않은 APK가 된다.
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.windowsize)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
