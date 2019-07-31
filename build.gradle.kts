import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.sbl"
version = "1.0-SNAPSHOT"

val kotlinVersion = "1.3.31"
val springBootVersion = "2.1.6.RELEASE"
val logbackVersion = "1.2.3"
val logstashVersion = "5.3"
val junitJupiterVersion = "5.4.2"
val mockkVersion = "1.9.3"
val wireMockVersion = "2.19.0"
val filformatVersion = "1.2019.07.09-13.14-92d3aa4f71f5"
val micrometerRegistryVersion = "1.1.2"
val tokenSupportVersion = "0.2.18"
val jacksonVersion = "2.9.9"
val jacksonDatabindVersion = "2.9.9.2"
val guavaVersion = "28.0-jre"
val swaggerVersion = "2.9.2"
val resilience4jVersion = "0.16.0"
val rxjavaVersion = "2.2.10"

val mainClass = "no.nav.sbl.sosialhjelpinnsynapi.ApplicationKt"

plugins {
    application
    kotlin("jvm") version "1.3.31"

    id("org.jetbrains.kotlin.plugin.spring") version "1.3.31"
    id("org.springframework.boot") version "2.1.6.RELEASE"
    id("io.spring.dependency-management") version "1.0.8.RELEASE"
}

application {
    mainClassName = mainClass
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

configurations {
    "compile" {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }
    "testCompile" {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
        exclude(group = "junit", module = "junit")
        exclude(group = "org.hamcrest", module = "hamcrest-library")
        exclude(group = "org.hamcrest", module = "hamcrest-core")
    }
}

dependencies {
    compile(kotlin("stdlib"))
    compile(kotlin("reflect"))

    compile("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    compile("org.springframework.boot:spring-boot-starter-jetty:$springBootVersion")
    compile("org.springframework.boot:spring-boot-starter-security:$springBootVersion")
    compile("org.springframework.boot:spring-boot-starter-actuator:$springBootVersion")
    compile("org.springframework.boot:spring-boot-starter-logging:$springBootVersion")

    compile("io.micrometer:micrometer-registry-prometheus:$micrometerRegistryVersion")

    compile("ch.qos.logback:logback-classic:$logbackVersion")
    compile("net.logstash.logback:logstash-logback-encoder:$logstashVersion")

    compile("no.nav.sbl.dialogarena:soknadsosialhjelp-filformat:$filformatVersion")

    compile("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    compile("no.nav.security:oidc-spring-support:$tokenSupportVersion")
    compile("io.springfox:springfox-swagger2:$swaggerVersion")
    compile("io.springfox:springfox-swagger-ui:$swaggerVersion")

    //spesifikke versjoner oppgradert etter ønske fra snyk
    compile("com.fasterxml.jackson.core:jackson-databind:$jacksonDatabindVersion")
    compile("com.google.guava:guava:$guavaVersion")

    //selftest
    compile("io.github.resilience4j:resilience4j-spring-boot2:$resilience4jVersion")
    compile("io.github.resilience4j:resilience4j-timelimiter:$resilience4jVersion")
    compile("io.github.resilience4j:resilience4j-rxjava2:$resilience4jVersion")
    compile("io.reactivex.rxjava2:rxjava:$rxjavaVersion")
    compile("org.springframework.boot:spring-boot-starter-aop:$springBootVersion")

//    Test dependencies
    testCompile("org.springframework.boot:spring-boot-starter-test:$springBootVersion") {
        exclude(group = "org.mockito", module = "mockito-core")
    }
    testCompile("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testCompile("com.github.tomakehurst:wiremock:$wireMockVersion")
    testCompile("no.nav.security:oidc-test-support:$tokenSupportVersion")
}

repositories {
    mavenCentral()
    jcenter()
    maven("https://plugins.gradle.org/m2/")
    maven("http://repo.spring.io/plugins-release/")
}

tasks {
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
