package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.parsing.FirstStageParsingResult
import com.strumenta.kolasu.parsing.KolasuToken
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.validation.Issue

class ParsingResultWithTokens<RootNode : KNode>(
    issues: List<Issue>,
    root: RootNode?,
    val tokens: List<KolasuToken>,
    code: String? = null,
    incompleteNode: com.strumenta.kolasu.model.Node? = null,
    firstStage: FirstStageParsingResult<*>? = null,
    time: Long? = null,
    source: Source? = null
) : ParsingResult<RootNode>(issues, root, code, incompleteNode, firstStage, time, source)
