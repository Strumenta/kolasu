package com.strumenta.kolasu.interop

import io.lionweb.lioncore.java.model.Node as LWNode

interface StarlasuSemanticEnricher {

    /**
     * Return the name of the languages on which this Semantic Enricher can operate.
     */
    fun supportedLanguages(): Set<String>

    fun setCodebaseAccess(codebaseAccess: CodebaseAccess)

    /**
     * @param codebaseFile the codebase file, serialized in LionWeb format.
     *
     * @return the AST with possibly resolved references and type annotations, in LionWebFormat.
     *         Return null when no changes have been made.
     */
    fun processCodebaseFile(codebaseFile: LWNode): LWNode?
}
