package com.strumenta.kolasu.javalib

import com.strumenta.kolasu.model.Node

data class Library(val books: List<Book>) : Node()
data class Book(val title: String, val numberOfPages: Int) : Node()
