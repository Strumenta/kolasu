package me.tomassetti.kolasu.validation

import me.tomassetti.kolasu.model.Position

data class Error(val message: String, val position: Position?)
