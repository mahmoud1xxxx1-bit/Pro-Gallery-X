package com.progalleryx.app

data class BrushPoint(val x: Float, val y: Float) {
    fun valid(): Boolean = x in 0f..1f && y in 0f..1f
}
data class BrushStroke(val points: List<BrushPoint>, val dark: Boolean = false, val width: Float = 0.01f) {
    fun valid(): Boolean = points.size in 1..2000 && points.all { it.valid() } && width in 0.003f..0.04f
}
