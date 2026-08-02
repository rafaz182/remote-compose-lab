pluginManagement {
    repositories {
        // google() vem primeiro e com filtro: é de onde saem o AGP e TODOS os
        // artefatos androidx.compose.remote.* (eles NÃO estão no Maven Central).
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // FAIL_ON_PROJECT_REPOS: proíbe declarar repositório dentro de um módulo.
    // Boa prática — garante que só existe UMA fonte de verdade de repositórios.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "compose-remote-lab"
include(":app")
