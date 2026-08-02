// Build script raiz.
//
// `apply false` significa: "carregue o plugin no classpath da build, mas não o
// aplique a ESTE projeto". Quem aplica de verdade é o :app. Isso mantém a raiz
// sem configuração e evita que o Gradle trate a pasta raiz como um módulo.
//
// Repare que NÃO existe plugin de Kotlin aqui. Desde o AGP 9.0 o Kotlin vem
// embutido no AGP (que hoje traz o KGP 2.2.10+). Um projeto Android moderno
// não declara mais a versão do Kotlin — um detalhe que ainda pega muita gente
// desprevenida, porque quase todo tutorial na internet é pré-AGP 9.
plugins {
    alias(libs.plugins.android.application) apply false

    // Estes dois existem por causa do módulo :server, que é JVM puro.
    //
    // PEGADINHA: o AGP 9 coloca o Kotlin Gradle Plugin no classpath da build
    // SEM versão declarada. Se o :server pedisse `alias(libs.plugins.kotlin.jvm)`
    // direto, o Gradle recusaria com:
    //   "the plugin is already on the classpath with an unknown version,
    //    so compatibility cannot be checked"
    // Declarar aqui na raiz, com apply false, resolve a versão uma única vez
    // para o build inteiro — e aí o :server pode aplicá-lo sem drama.
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
