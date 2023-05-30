package com.strumenta.kolasu.model

import java.io.Serializable

interface Destination

data class CompositeDestination(val elements: List<Destination>) : Destination, Serializable
data class TextFileDestination(val range: Range?) : Destination, Serializable

data class NodeDestination(val node: Node) : Destination
