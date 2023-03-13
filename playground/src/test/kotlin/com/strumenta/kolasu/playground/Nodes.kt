package com.strumenta.kolasu.playground

import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.ASTNode

data class ANode(override val name: String, val value: Int) : ASTNode(), Named

data class BNode(override val name: String, val value: Int) : ASTNode(), Named
