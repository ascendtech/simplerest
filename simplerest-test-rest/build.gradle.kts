import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    id("com.github.johnrengelman.shadow")
    `java-library`
}

val micronautVersion: String by project
val log4jVersion: String by project

dependencies {

    implementation("io.reactivex.rxjava2:rxjava:2.2.0")

    annotationProcessor("io.micronaut:micronaut-inject-java:$micronautVersion")
    implementation("io.micronaut:micronaut-http-client:$micronautVersion")
    implementation("io.micronaut:micronaut-http-server-netty:$micronautVersion")
    implementation("io.micronaut:micronaut-inject:$micronautVersion")
    implementation("io.micronaut:micronaut-runtime:$micronautVersion")

    compileOnly("io.micronaut:micronaut-inject-java:$micronautVersion")
    testImplementation("junit:junit:4.12")

    implementation("javax.annotation:javax.annotation-api:1.3.2")
    annotationProcessor("javax.annotation:javax.annotation-api:1.3.2")

    implementation("com.google.guava:guava:27.1-jre")
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-jul:$log4jVersion")

}

tasks.getByName<Zip>("shadowDistZip").archiveClassifier.set("shadow")
tasks.getByName<Tar>("shadowDistTar").archiveClassifier.set("shadow")

tasks.withType<ShadowJar> {
    mergeServiceFiles()
    isZip64 = true
}

application {
    mainClassName = "us.ascendtech.rest.ToDoApplication"
}

