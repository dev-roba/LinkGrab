import java.util.Properties
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.GradleException
import org.gradle.api.Action

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.personal.linkgrab"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.personal.linkgrab"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        create("release") {
            val propsFile = rootProject.file("keystore.properties")
            if (propsFile.exists()) {
                val props = Properties().apply {
                    propsFile.inputStream().use { load(it) }
                }
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            ndk {
                abiFilters += listOf("arm64-v8a", "x86_64")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
        }
        create("releaseTest") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
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
        jniLibs {
            // youtubedl-android ships uncompressed native libs; keep legacy packaging
            useLegacyPackaging = true
        }
    }
}

// Guard: fail release-variant tasks (but NOT releaseTest) if keystore.properties is missing
gradle.taskGraph.whenReady(object : Action<TaskExecutionGraph> {
    override fun execute(taskGraph: TaskExecutionGraph) {
        val releaseTasks = taskGraph.allTasks.filter { task: Task -> task.name.contains("Release") && !task.name.contains("ReleaseTest") }
        if (releaseTasks.any()) {
            val propsFile = rootProject.file("keystore.properties")
            if (!propsFile.exists()) {
                throw GradleException("keystore.properties not found. Run ./tools/gen-keys.sh to generate a release keystore, or use the debug-signed releaseTest build for testing.")
            }
        }
    }
})

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.2.21")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material.icons.core)
    implementation(libs.material.icons.extended)
    implementation(libs.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.androidx.work.runtime)
    implementation(libs.youtubedl.library)
    implementation(libs.youtubedl.ffmpeg)
    debugImplementation(libs.androidx.ui.tooling)
}
