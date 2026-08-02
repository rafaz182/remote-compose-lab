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
}
