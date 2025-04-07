package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Position
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.model.impl.EnumerationValue
import io.lionweb.lioncore.kotlin.BaseNode

import com.strumenta.starlasu.base.ASTLanguage;

class IssueNode : BaseNode(LIONWEB_VERSION_USED_BY_KOLASU) {
    var type: EnumerationValue? by property("type")
    var message: String? by property("message")
    var severity: EnumerationValue? by property("severity")
    var position: Position? by property("position")

    override fun getClassifier(): Concept {
        return ASTLanguage.getIssue()
    }
}
