package com.strumenta.kolasu.playground

import com.strumenta.kolasu.model.INode
import com.strumenta.kolasu.model.Named

data class ANode(override val name: String, val value: Int) : INode(), Named

data class BNode(override val name: String, val value: Int) : INode(), Named
