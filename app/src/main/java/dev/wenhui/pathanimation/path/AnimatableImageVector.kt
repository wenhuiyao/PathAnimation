package dev.wenhui.pathanimation.path

import androidx.annotation.DrawableRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.modifier.modifierLocalMapOf
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * An animatable image vector.
 *
 * To use it, provide an [ImageVector] to [imageRes], and it's animation path to [pathRes].
 *
 * @param pathOffset Extra offset match path with image vector
 */
@Composable
fun AnimatedImageVector(
    @DrawableRes imageRes: Int,
    @DrawableRes pathRes: Int,
    pathStrokeWidth: Dp,
    modifier: Modifier = Modifier,
    pathOffset: DpOffset = DpOffset.Zero,
    backgroundColor: Color = Color.LightGray,
    fillColor: Color = MaterialTheme.colorScheme.secondary,
) {
    val imageVector = ImageVector.vectorResource(imageRes)
    val animationVector = ImageVector.vectorResource(pathRes)
    val imagePath = remember(imageVector) { imageVector.toPath() }
    val animationPath = remember(animationVector) { animationVector.toPath() }
    val measurePolicy = remember(imagePath) { ImageVectorMeasurePolicy(imagePath) }
    Layout(
        // Must call imageVector first to setup ModifierLocalImageVectorNode for right side modifiers
        modifier = Modifier.imageVector(
            imagePath,
            animationPath,
            pathStrokeWidth,
            pathOffset,
            backgroundColor,
            fillColor
        ) then modifier,
        measurePolicy = measurePolicy
    )
}

private class ImageVectorMeasurePolicy(imagePath: Path) : MeasurePolicy {
    private val intrinsicBounds = imagePath.getBounds()
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        return with(constraints) {
            val width = if (hasFixedWidth) maxWidth else intrinsicBounds.width.roundToInt()
            val height = if (hasFixedHeight) maxHeight else intrinsicBounds.height.roundToInt()
            layout(width, height) {}
        }
    }
}

fun Modifier.imageVector(
    imagePath: Path,
    animationPath: Path,
    pathStrokeWidth: Dp,
    pathOffset: DpOffset,
    backgroundColor: Color,
    fillColor: Color,
) = this.graphicsLayer {
    // Enable offscreen composition to support blendMode
    compositingStrategy = CompositingStrategy.Offscreen
} then AnimatableImageVectorModifier(
    imagePath,
    animationPath,
    pathStrokeWidth,
    pathOffset,
    backgroundColor,
    fillColor
)

private data class AnimatableImageVectorModifier(
    private val imagePath: Path,
    private val animationPath: Path,
    private val pathStrokeWidth: Dp,
    private val pathOffset: DpOffset,
    private val backgroundColor: Color,
    private val fillColor: Color,
) : ModifierNodeElement<AnimatableImageVectorNode>() {
    override fun create(): AnimatableImageVectorNode =
        AnimatableImageVectorNode(
            imagePath,
            animationPath,
            pathStrokeWidth,
            pathOffset,
            backgroundColor,
            fillColor
        )

    override fun update(node: AnimatableImageVectorNode) {
        node.update(
            imagePath,
            animationPath,
            pathStrokeWidth,
            pathOffset,
            backgroundColor,
            fillColor
        )
    }
}

/**
 * This is the base node that handles scale path to fit measurement size, and blend animation with
 * the base path, the actual animation and seeking behavior are handled by sibling nodes.
 * See [ImageVectorScopeNode] for how the behavior is exposed to other nodes
 */
private class AnimatableImageVectorNode(
    private var imagePath: Path,
    private var animationPath: Path,
    private var pathStrokeWidth: Dp,
    private var pathOffset: DpOffset,
    private var backgroundColor: Color,
    private var fillColor: Color,
) : Modifier.Node(),
    ModifierLocalModifierNode,
    LayoutAwareModifierNode,
    DrawModifierNode {

    // Run into an issue where implement DrawModifierNode and LayoutModifierNode together will
    // make calling invalidateDraw() ineffective, is this a bug?

    private val segment = Path()
    private val pathMeasure = PathMeasure()

    private var imageBounds: Rect = Rect.Zero
    private var animationBounds: Rect = Rect.Zero
    private var centerOffset = Offset.Zero
    private var animationPathLength = 0f
    private var stroke: Stroke? = null

    var intrinsicBounds = imagePath.getBounds()
        private set

    val pathLength: Float
        get() = animationPathLength

    override val providedValues = modifierLocalMapOf(ModifierLocalImageVectorNode to this)

    fun update(
        imagePath: Path,
        animationPath: Path,
        pathStrokeWidth: Dp,
        pathOffset: DpOffset,
        backgroundColor: Color,
        fillColor: Color
    ) {
        this.imagePath = imagePath
        this.animationPath = animationPath
        this.pathStrokeWidth = pathStrokeWidth
        this.pathOffset = pathOffset
        this.backgroundColor = backgroundColor
        this.fillColor = fillColor
        intrinsicBounds = imagePath.getBounds()
        stroke = null
    }

    override fun onRemeasured(size: IntSize) {
        // Scale path to fit size, to keep image's aspect ratio with a fixed width/height,
        // use Modifier.fitHeight or Modifier.fitWidth
        val tempBounds = imagePath.getBounds()
        val scaleX = size.width / tempBounds.width
        val scaleY = size.height / tempBounds.height
        val matrix = Matrix()
        matrix.translate(
            (size.width - tempBounds.width * scaleX) / 2f,
            (size.height - tempBounds.height * scaleY) / 2f
        )
        matrix.scale(scaleX, scaleY)
        imagePath.transform(matrix)
        animationPath.transform(matrix)

        imageBounds = imagePath.getBounds()
        animationBounds = animationPath.getBounds()
        // Try to match animation path with the base image path
        centerOffset =
            imageBounds.center - animationBounds.center + pathOffset.toOffset(requireDensity())

        pathMeasure.setPath(animationPath, forceClosed = false)
        animationPathLength = pathMeasure.length
        // Always show the filled path when it's not animating
        drawSegment(0f, animationPathLength)
    }

    fun drawSegment(startDist: Float, endDist: Float) {
        segment.rewind()
        pathMeasure.getSegment(
            maxOf(0f, startDist),
            minOf(endDist, animationPathLength),
            segment,
            startWithMoveTo = true
        )
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        drawPath(imagePath, color = backgroundColor)
        translate(centerOffset.x, centerOffset.y) {
            // This is the main step to blend the animation path into the base path using
            // BlendMode.SrcIn, to see the ugly unmasked path, comment out blendMOde :-)
            drawPath(
                segment,
                color = fillColor,
                style = ensureStroke(),
                blendMode = BlendMode.SrcIn
            )
        }
    }

    private fun Density.ensureStroke(): Stroke {
        if (stroke == null) {
            stroke = Stroke(width = pathStrokeWidth.toPx())
        }
        return checkNotNull(stroke)
    }
}

private val ModifierLocalImageVectorNode = modifierLocalOf<AnimatableImageVectorNode?> { null }

/**
 * The base class for all nodes that wants to modify [AnimatableImageVectorNode]
 */
abstract class ImageVectorScopeNode : Modifier.Node(), ModifierLocalModifierNode {
    private val imageVector: AnimatableImageVectorNode?
        get() = if (isAttached) ModifierLocalImageVectorNode.current else null

    protected val pathLength: Float
        get() = imageVector?.pathLength ?: 0f

    protected val intrinsicBounds: Rect
        get() = imageVector?.intrinsicBounds ?: Rect.Zero

    /**
     * Draw a path segment on top of the base image path
     */
    protected fun drawSegment(startDist: Float, endDist: Float) {
        imageVector?.drawSegment(startDist, endDist)
    }
}


