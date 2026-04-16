// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    //id("com.android.application") version "8.2.0" apply false
    //id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    // 添加 OpenAPI Generator 插件
    id("org.openapi.generator") version "7.2.0" apply false
}