package dev.wenhui.pathanimation.path

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

fun Modifier.animateImageVector(active: Boolean, segmentLength: Dp) =
    this then PathAnimationModifier(active, segmentLength)

private data class PathAnimationModifier(
    private val active: Boolean,
    private val segmentLength: Dp
) : ModifierNodeElement<PathAnimationNode>() {
    override fun create(): PathAnimationNode = PathAnimationNode(active, segmentLength)

    override fun update(node: PathAnimationNode) {
        node.update(active, segmentLength)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "pathAnimation"
        properties["active"] = active
        properties["segmentLength"] = segmentLength
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class PathAnimationNode(
    private var active: Boolean,
    private var segmentLength: Dp
) : ImageVectorScopeNode() {
    private var shimmerAnimationJob: Job? = null
    private var fillAnimationJob: Job? = null

    private val animationSpec: SpringSpec<Float> by lazy(LazyThreadSafetyMode.NONE) {
        val visibilityThreshold = with(requireDensity()) { 0.5.dp.toPx() }
        spring(stiffness = 40f, visibilityThreshold = visibilityThreshold)
    }

    fun update(active: Boolean, segmentLength: Dp) {
        this.segmentLength = segmentLength
        if (this.active != active) {
            this.active = active
            if (active) startAnimation() else stopAndRunFillAnimation()
        }
    }

    override fun onAttach() {
        if (active) {
            sideEffect { startAnimation() }
        } else {
            // Show filled path on inactive mode
            drawSegment(0f, pathLength)
        }
    }

    private fun startAnimation() {
        if (!isAttached || shimmerAnimationJob?.isActive == true) {
            return
        }
        startRepeatAnimation()
    }


    private fun startRepeatAnimation() {
        val segmentLengthPx = with(requireDensity()) { segmentLength.toPx() }
        shimmerAnimationJob = coroutineScope.launch {
            fillAnimationJob?.cancelAndJoin()
            val startValue = -segmentLengthPx
            val endValue = pathLength
            // Always run the animation in a full cycle forward and reverse
            // First run forward animation
            runAnimation(startValue, endValue, segmentLengthPx)
            // After forward animation finished, run reverse animation
            runAnimation(endValue, startValue, segmentLengthPx)
        }.also { job ->
            job.invokeOnCompletion {
                if (isAttached && active && !job.isCancelled) {
                    startRepeatAnimation()
                }
            }
        }
    }

    private suspend fun runAnimation(
        initialValue: Float,
        targetValue: Float,
        segmentLengthPx: Float
    ) {
        Animatable(initialValue).animateTo(targetValue, animationSpec) {
            drawSegment(value, value + segmentLengthPx)
        }
    }

    private fun stopAndRunFillAnimation() {
        if (!isAttached) return
        fillAnimationJob = coroutineScope.launch {
            // Wait until shimmering animation finished
            shimmerAnimationJob?.join()

            // Start running fill animation
            Animatable(0f).animateTo(
                targetValue = pathLength,
                animationSpec = animationSpec
            ) {
                drawSegment(0f, value)
            }
        }
    }
}