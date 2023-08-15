package dev.wenhui.pathanimation

import androidx.compose.ui.Modifier

inline fun Modifier.conditional(
    condition: Boolean,
    modifier: Modifier.() -> Modifier
) = if (condition) this then this.modifier() else this
