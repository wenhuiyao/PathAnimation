package dev.wenhui.pathanimation.path

import android.annotation.SuppressLint
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.unit.Dp

fun Modifier.seekPath(progress: Float, segmentLength: Dp) =
    this then SeekPathModifier(progress, segmentLength)

@SuppressLint("ModifierNodeInspectableProperties")
private data class SeekPathModifier(private val progress: Float, private val segmentLength: Dp) :
    ModifierNodeElement<SeekPathNode>() {
    override fun create() = SeekPathNode(progress, segmentLength)
    override fun update(node: SeekPathNode) {
        node.update(progress, segmentLength)
    }
}

private class SeekPathNode(
    private var progress: Float,
    private var segmentLength: Dp
) : ImageVectorScopeNode() {

    fun update(progress: Float, segmentLength: Dp) {
        this.progress = progress
        this.segmentLength = segmentLength
        updateProgress(progress)
    }

    override fun onAttach() {
        updateProgress(progress)
    }

    private fun updateProgress(progress: Float) {
        val segmentLengthPx = with(requireDensity()) { segmentLength.toPx() }
        val startDist = mapValueFromRangeToRange(
            value = progress,
            fromLow = 0f,
            fromHigh = 1f,
            toLow = -segmentLengthPx,
            toHigh = pathLength
        )
        drawSegment(startDist, startDist + segmentLengthPx)
    }
}