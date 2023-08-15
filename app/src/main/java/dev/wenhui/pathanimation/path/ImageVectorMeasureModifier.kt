package dev.wenhui.pathanimation.path

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

/**
 * Set a fixed height, and automatically calculate width to match the image vector's aspect ratio
 */
fun Modifier.fixedHeightMatchRatio(height: Dp) = this then ImageVectorMeasureOptionElement(
    MeasureOption.FixedHeightMatchRatio(height)
)

/**
 * Set a fixed width, and automatically calculate height to match the image vector's aspect ratio
 */
fun Modifier.fixedWidthMatchRatio(width: Dp) = this then ImageVectorMeasureOptionElement(
    MeasureOption.FixedWidthMatchRatio(width)
)

private sealed interface MeasureOption {
    data class FixedHeightMatchRatio(val height: Dp) : MeasureOption
    data class FixedWidthMatchRatio(val width: Dp) : MeasureOption
}

private data class ImageVectorMeasureOptionElement(
    val measureOption: MeasureOption
) : ModifierNodeElement<ImageVectorMeasureOptionNode>() {
    override fun create(): ImageVectorMeasureOptionNode =
        ImageVectorMeasureOptionNode(measureOption)

    override fun update(node: ImageVectorMeasureOptionNode) {
        node.update(measureOption)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "imageVectorMeasureOption"
        properties["measureOption"] = measureOption
    }
}

private class ImageVectorMeasureOptionNode(
    private var measureOption: MeasureOption,
) : ImageVectorScopeNode(), LayoutModifierNode {

    fun update(measureOption: MeasureOption) {
        this.measureOption = measureOption
        invalidateMeasurement()
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val measureOption = this@ImageVectorMeasureOptionNode.measureOption
        val width: Int
        val height: Int
        val ratio = intrinsicBounds.width / intrinsicBounds.height
        when (measureOption) {
            is MeasureOption.FixedHeightMatchRatio -> {
                height = measureOption.height.roundToPx()
                width = (height * ratio).roundToInt()
            }

            is MeasureOption.FixedWidthMatchRatio -> {
                width = measureOption.width.roundToPx()
                height = (width / ratio).roundToInt()
            }
        }
        val placeable = measurable.measure(Constraints.fixed(width, height))
        return layout(width, height) {
            placeable.place(0, 0)
        }
    }
}