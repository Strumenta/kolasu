package com.strumenta.kolasu.emf.multipkg

import com.strumenta.kolasu.emf.multipkg.subpackage.ExampleASTClassChild
import com.strumenta.kolasu.model.ASTNode

class ExampleASTClassParent(val elements: List<ExampleASTClassChild>) : ASTNode()
