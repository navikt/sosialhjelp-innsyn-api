import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.sbl"
version = "1.0-SNAPSHOT"

val kotlinVersion = "1.3.31"
val springBootVersion = "2.1.4.RELEASE"
val logbackVersion = "1.2.3"
val logstashVersion = "5.3"
val junitJupiterVersion = "5.4.2"
val mockkVersion = "1.9.3"
val wireMockVersion = "2.19.0"
val filformatVersion = "1.2019.05.08-16.27-0a95b4468f3d"
val micrometerRegistryVersion = "1.1.2"
val tokenSupportVersion = "0.2.18"
val jacksonVersion = "2.9.9"

val mainClass = "no.nav.sbl.sosialhjelpinnsynapi.ApplicationKt"

plugins {
    application
    kotlin("jvm") version "1.3.31"

    id("org.jetbrains.kotlin.plugin.spring") version "1.3.31"
    id("org.springframework.boot") version "2.1.4.RELEASE"
    id("io.spring.dependency-management") version "1.0.7.RELEASE"
}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

application {
    mainClassName = mainClass
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    compile(kotlin("stdlib"))
    compile(kotlin("reflect"))

    compile("org.springframework.boot:spring-boot-starter-web:$springBootVersion") {
        exclude(module = "spring-boot-starter-tomcat")
    }
    compile("org.springframework.boot:spring-boot-starter-jetty:$springBootVersion")
    compile("org.springframework.boot:spring-boot-starter-actuator:$springBootVersion")
    compile("io.micrometer:micrometer-registry-prometheus:$micrometerRegistryVersion")
    compile("org.springframework.boot:spring-boot-starter-logging:$springBootVersion") {
        exclude(module = "commons-logging")
    }
    compile("ch.qos.logback:logback-classic:$logbackVersion")
    compile("net.logstash.logback:logstash-logback-encoder:$logstashVersion")

    compile("no.nav.sbl.dialogarena:soknadsosialhjelp-filformat:$filformatVersion")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    compile("no.nav.security:oidc-spring-support:$tokenSupportVersion") {
        exclude(module = "spring-boot-starter-tomcat")
    }

    testCompile("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testCompile("com.github.tomakehurst:wiremock:$wireMockVersion") {
        exclude(group = "junit")
    }
    testCompile("no.nav.security:oidc-test-support:$tokenSupportVersion")
}

repositories {
    mavenCentral()
    jcenter()
    maven("https://plugins.gradle.org/m2/")
    maven("http://repo.spring.io/plugins-release/")
}

tasks {
    create("printVersion") {
        doLast {
            println(project.version)
        }
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    withType<Test> {
        useJUnitPlatform {
            includeEngines("junit-jupiter")
        }
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
