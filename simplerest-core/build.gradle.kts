plugins {
    id("us.ascendtech.gwt.lib")
}

dependencies {
    api("javax.ws.rs:jsr311-api:1.1.1")
    api("javax.inject:javax.inject:1")
    implementation("com.google.elemental2:elemental2-dom:1.1.0")
}

description = "simplerest-core"
