package com.smeup.rpgparser.parsing.ast

import com.strumenta.kolasu.model.ASTNode
import com.strumenta.kolasu.model.Range

abstract class Directive(specifiedRange: Range? = null) : ASTNode(specifiedRange)

data class DeceditDirective(val format: String, val specifiedRange: Range? = null) : Directive(
    specifiedRange
)

data class ActivationGroupDirective(val type: ActivationGroupType, val specifiedRange: Range? = null) :
    Directive(specifiedRange)

sealed class ActivationGroupType

object CallerActivationGroup : ActivationGroupType()

object NewActivationGroup : ActivationGroupType()

data class NamedActivationGroup(val groupName: String) : ActivationGroupType()
