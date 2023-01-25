package com.strumenta.kolasu.playground

import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node

data class ANode(override val name: String, val value: Int) : Node(), Named
