package com.strumenta.kolasu.playground

import com.strumenta.kolasu.model.ASTNode
import com.strumenta.kolasu.model.Named

data class ANode(override val name: String, val value: Int) : ASTNode(), Named

data class BNode(override val name: String, val value: Int) : ASTNode(), Named
