object Apps {
    const val compileSdk = 28
    const val minSdk = 21
    const val targetSdk = 28
    const val versionCode = 1
    const val versionName = "1.0.0"
}

object Android {
    const val TestRunner = "org.robolectric.RobolectricTestRunner"
}

object Versions {
    const val gradle = "4.1.0"
    const val kotlin = "1.4.10"
    const val coroutines = "1.3.9-native-mt-2"
    const val serialization = "1.0.0-RC2"
    const val ktor = "1.4.1"
    const val TestCore = "1.2.0"
    const val RoboElectric = "4.3.1"
}

object Libs {
    const val kotlinGradle = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val serialization = "org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}"
    const val gradleAndroid = "com.android.tools.build:gradle:${Versions.gradle}"
    const val kotlinAndroid = "org.jetbrains.kotlin:kotlin-android-extensions:${Versions.kotlin}"

    /* Common */
    const val ktorCore = "io.ktor:ktor-client-core:${Versions.ktor}"
    const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
    const val serializationCore = "org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.serialization}"
    const val ktorSerializationCore = "io.ktor:ktor-client-serialization:${Versions.ktor}"

    /* Android */
    const val ktorAndroid = "io.ktor:ktor-client-android:${Versions.ktor}"

    /* iOS */
    const val ktorIOS = "io.ktor:ktor-client-ios:${Versions.ktor}"
}

object TestLibs {
    const val TestCore = "androidx.test:core:${Versions.TestCore}"
    const val RoboElectrics = "org.robolectric:robolectric:${Versions.RoboElectric}"
}

object Exclude {
    const val GoogleAutoService = "com.google.auto.service"
    const val AutoService = "auto-service"
}
