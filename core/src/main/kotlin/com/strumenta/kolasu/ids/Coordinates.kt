package com.strumenta.kolasu.ids

sealed class Coordinates

object RootCoordinates : Coordinates()

data class NonRootCoordinates(
    val containerID: String,
    val containmentName: String,
    val indexInContainment: Int
) : Coordinates()
