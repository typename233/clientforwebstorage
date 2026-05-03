plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    //id("com.android.application")
    //id("org.jetbrains.kotlin.android")
    id("org.openapi.generator")  // 应用插件
}

android {
    namespace = "com.example.clientforwebstorage"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.clientforwebstorage"
        minSdk = 24
        targetSdk = 36
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
    // ... 其他配置
    sourceSets {
        getByName("main") {
            java.srcDir("${buildDir}/generated/openapi/src/main/kotlin")
        }
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    // 后续生成代码所需的依赖
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // 如果使用 Moshi 代替 Gson
    // implementation("com.squareup.retrofit2:converter-moshi:2.9.0")

    // Kotlin 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Lifecycle + ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.0")

    // Retrofit + Gson + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    ////////////////
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}
// OpenAPI Generator 配置
openApiGenerate {
    // 生成器类型，使用 kotlin 生成 Kotlin 代码
    generatorName.set("kotlin")

    // 输入的 OpenAPI 规范文件路径
    inputSpec.set("${projectDir}/specs/netdisk_apifox_openapi_v1.json")

    // 输出目录
    outputDir.set("${buildDir}/generated/openapi")

    // API 接口包名
    apiPackage.set("com.example.netdisk.api")

    // 数据模型包名
    modelPackage.set("com.example.netdisk.model")

    // 配置选项
    configOptions.set(mapOf(
        // 序列化库：gson 或 moshi
        "serializationLibrary" to "gson",
        // 使用 Kotlin 协程（suspend 函数）
        "useCoroutines" to "true",
        // 日期库
        "dateLibrary" to "java8",
        // 请求/响应使用密封类包装（可选）
        "requestDateConverter" to "toString",
        // 是否生成 @JvmOverloads 注解
        "generateAliasAsModel" to "false",
        // 模型类使用 data class
        "library" to "jvm-retrofit2"
    ))

    // 全局参数映射（可定义一些替换变量，一般不需要）
    // globalProperties.set(mapOf(...))

    // 忽略文件覆盖警告（每次生成强制覆盖）
    //ignoreFileOverride.set(".openapi-generator-ignore")
}