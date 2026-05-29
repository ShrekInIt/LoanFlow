pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "LoanFlow"

include("application-service")
include("scoring-service")
include("contracts")