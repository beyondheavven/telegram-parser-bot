plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
}

group = "com.telegram"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.cors)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.routingOpenapi)
    implementation(ktorLibs.server.swagger)
    implementation(libs.logback.classic)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.10.1")

    implementation(platform("it.tdlight:tdlight-java-bom:3.5.2+td.1.8.64"))
    implementation("it.tdlight:tdlight-java")
    implementation("it.tdlight:tdlight-natives:_:linux_amd64_gnu_ssl3")

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
