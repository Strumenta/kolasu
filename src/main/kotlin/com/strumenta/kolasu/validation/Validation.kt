package com.strumenta.kolasu.validation

import com.strumenta.kolasu.model.Position

enum class IssueType {
    LEXICAL,
    SYNTACTIC,
    SEMANTIC
}

data class Issue(val type: IssueType, val message: String, val position: Position? = null) {

    companion object {
        fun lexical(message: String, position: Position? = null) : Issue = Issue(IssueType.LEXICAL, message, position)
        fun syntactic(message: String, position: Position? = null) : Issue = Issue(IssueType.SYNTACTIC, message, position)
        fun semantic(message: String, position: Position? = null) : Issue = Issue(IssueType.SEMANTIC, message, position)
    }

}
