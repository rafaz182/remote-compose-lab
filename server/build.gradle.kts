plugins {
    // Módulo JVM puro. O Kotlin embutido do AGP 9 só serve para módulos
    // Android — aqui precisamos do plugin kotlin-jvm de sempre.
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    // Sem `toolchain { }` de propósito: exigir uma JDK 17 específica faria o
    // Gradle tentar baixá-la (ou falhar, se não houver repositório de toolchain
    // configurado). Aqui apenas pedimos bytecode 17, compilado por qualquer JDK
    // 17+ que estiver instalada.
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("dev.rafaz.remotecomposelab.server.ServidorKt")
}

// Tarefa auxiliar: roda só a sonda, sem subir o servidor.
tasks.register<JavaExec>("runSonda") {
    group = "verification"
    description = "Prova que dá para gerar um documento Remote Compose em JVM pura."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dev.rafaz.remotecomposelab.server.SondaKt")
}

// Disseca um documento: tabela de opcodes + dump hexadecimal anotado.
tasks.register<JavaExec>("runDissecar") {
    group = "verification"
    description = "Imprime a tabela de opcodes e um dump hexadecimal de documentos reais."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dev.rafaz.remotecomposelab.server.DissecarKt")
}

dependencies {
    // ─────────────────────────────────────────────────────────────────────
    // A PROVA DE FOGO DESTE MÓDULO
    //
    // Estas três linhas são a tese inteira do backend. Repare no que NÃO
    // está aqui: nada de `remote-creation-compose`, nada de player, nada
    // de Android. Só JAR puro.
    //
    //   remote-core          -> o formato do documento
    //   remote-creation-core -> a DSL imperativa que escreve o documento
    //   remote-creation-jvm  -> o plugue que faz isso funcionar fora do Android
    //
    // Se este módulo compila e roda, está provado que um servidor consegue
    // gerar UI de verdade. Era só teoria em docs/02 até agora.
    // ─────────────────────────────────────────────────────────────────────
    implementation(libs.androidx.remote.core)
    implementation(libs.androidx.remote.creation.core)
    implementation(libs.androidx.remote.creation.jvm)

    // ---- Ktor ----
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.cors)
    implementation(libs.logback)
}
