package ru.sliva.userbot

import java.math.RoundingMode

fun Iterable<String?>.containsIgnoreCase(other: String) = any { it.equals(other, ignoreCase = true) }

fun Number.round(scale: Int) = toFloat().toBigDecimal().setScale(scale, RoundingMode.UP).toFloat()

fun Number.toPlainString() = toFloat().toBigDecimal().stripTrailingZeros().toPlainString()