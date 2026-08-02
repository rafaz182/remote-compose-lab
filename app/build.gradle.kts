plugins {
    // A partir do AGP 9.0 o suporte a Kotlin é EMBUTIDO no próprio AGP.
    // Aplicar 'org.jetbrains.kotlin.android' aqui causa erro de build:
    //   "The 'org.jetbrains.kotlin.android' plugin is no longer required since AGP 9.0"
    alias(libs.plugins.android.application)

    // Já o plugin do COMPILADOR do Compose continua obrigatório — o AGP não o
    // aplica sozinho. Sem ele, `buildFeatures { compose = true }` falha com:
    //   "Starting in Kotlin 2.0, the Compose Compiler Gradle plugin is required"
    // Desde o Kotlin 2.0 esse compilador é versionado junto com o Kotlin
    // (e não mais com a versão do Compose).
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.rafaz.remotecomposelab"

    // Precisa ser 37+: o Compose BOM 2026.06.01 traz bibliotecas compiladas
    // contra a API 37 e o AGP recusa compilar contra uma API menor.
    // Instale com: sdkmanager "platforms;android-37.1"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.rafaz.remotecomposelab"

        // ATENÇÃO — pegadinha real do Remote Compose, descoberta na marra
        // (o merge de manifesto falhou duas vezes até chegarmos aqui):
        //
        //   as notas de versão dizem que o minSdk caiu para 23 na alpha04,
        //   mas os artefatos reais declaram outra coisa:
        //     remote-player-view        -> minSdk 26
        //     remote-creation-compose   -> minSdk 29   <-- o mais restritivo
        //
        // Portanto, na prática: Remote Compose = Android 10 (API 29) para cima.
        // Não confie na nota de versão; confie no AndroidManifest do AAR.
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        // Isto é o que liga o compilador do Compose no AGP 9.
        compose = true
    }
}

// ATENÇÃO: com built-in Kotlin, o bloco `kotlin` é de NÍVEL SUPERIOR — fica
// FORA do bloco `android`. Colocá-lo dentro não compila.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // ---- Casca do app: Compose "normal" ----
    // É a UI que VOCÊ já conhece: a navegação entre as aulas, os textos
    // explicativos, os botões. Nada disso é Remote Compose.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // ---- O objeto de estudo ----
    // CREATION: ESCREVE o documento (DSL @Composable: RemoteColumn, RemoteText…)
    implementation(libs.androidx.remote.creation.compose)
    // PLAYER: LÊ e RENDERIZA o documento
    implementation(libs.androidx.remote.player.compose)
    implementation(libs.androidx.remote.player.core)
    // CORE: o formato em si (CoreDocument, WireBuffer) — usaremos para
    // dissecar os bytes na aula final
    implementation(libs.androidx.remote.core)
    // CREATION-CORE: JAR puro, DSL imperativa — é o que um BACKEND usaria
    implementation(libs.androidx.remote.creation.core)
    // TOOLING: @Preview de conteúdo Remote Compose no Android Studio
    implementation(libs.androidx.remote.tooling.preview)

    // ---- Cliente HTTP, para buscar documentos no módulo :server ----
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
}
