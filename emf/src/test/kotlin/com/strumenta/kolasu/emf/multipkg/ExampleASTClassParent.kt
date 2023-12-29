package com.strumenta.kolasu.emf.multipkg

import com.strumenta.kolasu.emf.multipkg.subpackage.ExampleASTClassChild
import com.strumenta.kolasu.model.Node

class ExampleASTClassParent(
    val elements: List<ExampleASTClassChild>,
) : Node()
