package com.strumenta.kolasu.validation

import com.strumenta.kolasu.model.Range

enum class IssueType {
    LEXICAL,
    SYNTACTIC,
    SEMANTIC,
    TRANSLATION
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
    val range: Range? = null
) {

    companion object {
        fun lexical(
            message: String,
            severity: IssueSeverity = IssueSeverity.ERROR,
            range: Range? = null
        ): Issue = Issue(IssueType.LEXICAL, message, severity, range)
        fun syntactic(
            message: String,
            severity: IssueSeverity = IssueSeverity.ERROR,
            range: Range? = null
        ): Issue = Issue(IssueType.SYNTACTIC, message, severity, range)
        fun semantic(
            message: String,
            severity: IssueSeverity = IssueSeverity.ERROR,
            range: Range? = null
        ): Issue = Issue(IssueType.SEMANTIC, message, severity, range)
        fun translation(
            message: String,
            severity: IssueSeverity = IssueSeverity.ERROR,
            range: Range? = null
        ): Issue = Issue(IssueType.TRANSLATION, message, severity, range)
    }
}
