plugins {
    id("com.diffplug.spotless") version "6.25.0" apply false
}

allprojects {
    group = "com.limitra"
    version = "0.1.0"
    repositories { mavenCentral() }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")
}