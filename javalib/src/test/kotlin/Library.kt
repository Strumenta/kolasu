package com.strumenta.kolasu.javalib

import com.strumenta.kolasu.model.ASTNode
import com.strumenta.kolasu.model.NodeType

data class Library(val books: List<Book>, val team: List<TeamMember> = emptyList()) : ASTNode()

@NodeType
interface TeamMember
data class Book(val title: String, val numberOfPages: Int) : ASTNode()

data class Person(val seniority: Int, val name: String) : ASTNode(), TeamMember
