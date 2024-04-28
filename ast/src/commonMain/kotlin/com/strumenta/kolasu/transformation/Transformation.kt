package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.Origin

internal sealed class ParameterValue

internal class PresentParameterValue(
    val value: Any?,
) : ParameterValue()

internal data object AbsentParameterValue : ParameterValue()

fun asOrigin(source: Any): Origin? = if (source is Origin) source else null
