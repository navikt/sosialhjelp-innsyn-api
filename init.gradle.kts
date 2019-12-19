allprojects {
    buildscript {
        repositories {
            maven("https://repo.adeo.no/repository/maven-central")
        }
    }
    repositories {
        maven("https://repo.adeo.no/repository/maven-central")
        mavenCentral()
    }
}

