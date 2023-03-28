package com.strumenta.kolasu.model.lionweb

import com.strumenta.kolasu.model.ASTNode
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses

val KClass<*>.isEnum
    get() = this.allSuperclasses.contains(Enum::class)

val KClass<*>.isInterface
    get() = this.java.isInterface

val KClass<*>.isASTNode
    get() = this.allSuperclasses.contains(ASTNode::class)
