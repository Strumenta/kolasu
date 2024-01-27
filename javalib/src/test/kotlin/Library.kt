package com.strumenta.kolasu.javalib

import com.strumenta.kolasu.ast.NodeLike
import com.strumenta.kolasu.model.Node

data class Library(
    val books: List<Book>,
    val team: List<TeamMember> = emptyList(),
) : Node()

interface TeamMember : NodeLike

data class Book(
    val title: String,
    val numberOfPages: Int,
) : Node()

data class Person(
    val seniority: Int,
    val name: String,
) : Node(),
    TeamMember
