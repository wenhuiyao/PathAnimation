package dev.wenhui.pathanimation.path

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset

fun DpOffset.toOffset(density: Density): Offset {
    return with(density) {
        Offset(x = x.toPx(), y = y.toPx())
    }
}