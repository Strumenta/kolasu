package com.strumenta.kolasu.validation

import com.strumenta.kolasu.model.Position

enum class IssueType {
    LEXICAL,
    SYNTACTIC,
    SEMANTIC
}

enum class IssueSeverity {
    ERROR,
    WARNING,
    INFO
}

data class Issue(
    val type: IssueType,
    val message: String,
    val severity: IssueSeverity = IssueSeverity.ERROR,
    val position: Position? = null
) {

    companion object {
        fun lexical(
            message: String,
            severity: IssueSeverity = IssueSeverity.ERROR,
            position: Position? = null
        ): Issue = Issue(IssueType.LEXICAL, message, severity, position)
        fun syntactic(
            message: String,
            severity: IssueSeverity = IssueSeverity.ERROR,
            position: Position? = null
        ): Issue = Issue(IssueType.SYNTACTIC, message, severity, position)
        fun semantic(
            message: String,
            severity: IssueSeverity = IssueSeverity.ERROR,
            position: Position? = null
        ): Issue = Issue(IssueType.SEMANTIC, message, severity, position)
    }
}
