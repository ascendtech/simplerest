plugins {
    java
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "6.1.0" apply false
    id("us.ascendtech.gwt.lib") version "0.5.9" apply false
    id("us.ascendtech.gwt.modern") version "0.5.9" apply false
}

defaultTasks("build")

subprojects {

    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        //withSourcesJar()
    }

    val sourcesJar = tasks.register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceSets.getByName("main").allSource)
    }

    artifacts.add("archives", sourcesJar)

    defaultTasks("build")
    group = "us.ascendtech.simplerest"


    repositories {
        mavenLocal()
        mavenCentral()
    }

    configurations.all {
        // check for updates every build more than 10 minutes apart (for snapshots)
        resolutionStrategy.cacheChangingModulesFor(10, TimeUnit.MINUTES)
    }

    tasks.withType<JavaCompile> {
        options.isDebug = true
        options.debugOptions.debugLevel = "source,lines,vars"
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }
    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    }

    sourceSets {
        main {
            java {
                srcDir("src/main/java")
            }
            resources {
                srcDir("src/main/java")
            }
        }
    }

    publishing {
        publications.create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    tasks.withType<JavaCompile>() {
        options.encoding = "UTF-8"
    }


}