package com.strumenta.kolasu.playground

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Named

data class ANode(override val name: String, val value: Int) : Node(), Named

data class BNode(override val name: String, val value: Int) : Node(), Named
