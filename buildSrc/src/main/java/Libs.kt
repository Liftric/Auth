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
    const val gradle = "4.2.1"
    const val kotlin = "1.5.20"
    const val npmPublish = "2.0.3"
    const val definitions = "4.9.1"
    const val coroutines = "1.5.0-native-mt"
    const val serialization = "1.2.1"
    const val ktor = "1.6.1"
    const val TestCore = "1.2.0"
    const val RoboElectric = "4.5.1"
}

object Libs {
    const val serialization = "org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}"
    const val gradleAndroid = "com.android.tools.build:gradle:${Versions.gradle}"

    /* Common */
    const val ktorCore = "io.ktor:ktor-client-core:${Versions.ktor}"
    const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
    const val serializationCore = "org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.serialization}"
    const val ktorSerializationCore = "io.ktor:ktor-client-serialization:${Versions.ktor}"

    /* Android */
    const val ktorAndroid = "io.ktor:ktor-client-android:${Versions.ktor}"

    /* JVM */
    const val ktorJvm = "io.ktor:ktor-client-cio:${Versions.ktor}"

    /* iOS */
    const val ktorIOS = "io.ktor:ktor-client-ios:${Versions.ktor}"

    /* JS */
    const val ktorJS = "io.ktor:ktor-client-js:${Versions.ktor}"
}

object TestLibs {
    const val TestCore = "androidx.test:core:${Versions.TestCore}"
    const val RoboElectrics = "org.robolectric:robolectric:${Versions.RoboElectric}"
}

object Exclude {
    const val GoogleAutoService = "com.google.auto.service"
    const val AutoService = "auto-service"
}
