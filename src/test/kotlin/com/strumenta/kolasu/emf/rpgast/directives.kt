package com.smeup.rpgparser.parsing.ast

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Position

abstract class Directive(@Transient override val specifiedPosition: Position? = null) : Node(specifiedPosition)

data class DeceditDirective(val format: String, override val specifiedPosition: Position? = null) : Directive(specifiedPosition)

data class ActivationGroupDirective(val type: ActivationGroupType, override val specifiedPosition: Position? = null) :
    Directive(specifiedPosition)

sealed class ActivationGroupType

object CallerActivationGroup : ActivationGroupType()

object NewActivationGroup : ActivationGroupType()

data class NamedActivationGroup(val groupName: String) : ActivationGroupType()
