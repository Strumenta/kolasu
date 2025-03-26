package com.strumenta.kolasu.interop

interface StarlasuSemanticEnricher {

    /**
     * Return the name of the languages on which this Semantic Enricher can operate.
     */
    fun supportedLanguages() : Set<String>

    /**
     * Receive the AST, in LionWeb format.
     *
     * @return the AST with possibly resolved references and type annotations, in LionWebFormat.
     */
    fun processAST(codebaseName: String, relativePath: String, ast: String) : String

}