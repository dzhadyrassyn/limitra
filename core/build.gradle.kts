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
        // Use Google Java Format
        googleJavaFormat("1.17.0").aosp()
        // Which files to check
        target("src/**/*.java")
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> { useJUnitPlatform() }