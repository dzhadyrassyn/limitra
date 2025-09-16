plugins {

}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories { mavenCentral() }

spotless {
    java {
        googleJavaFormat("1.17.0").aosp()
        target("src/**/*.java")
    }
}

dependencies {
    implementation(project(":core"))
}