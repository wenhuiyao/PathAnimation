package dev.wenhui.pathanimation.path

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath

fun ImageVector.toPath(dst: Path) {
    dst.reset()
    return dst.addVectorNode(root)
}

fun ImageVector.toPath(): Path {
    return Path().apply { addVectorNode(root) }
}

private fun Path.addVectorNode(node: VectorNode) {
    when (node) {
        is VectorGroup -> {
            node.iterator().forEach { addVectorNode(it) }
        }

        is VectorPath -> {
            addPath(node.pathData.toPath())
        }
    }
}
