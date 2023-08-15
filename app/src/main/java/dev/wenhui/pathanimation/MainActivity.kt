package dev.wenhui.pathanimation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.wenhui.pathanimation.path.AnimatedImageVector
import dev.wenhui.pathanimation.path.animateImageVector
import dev.wenhui.pathanimation.path.fixedHeightMatchRatio
import dev.wenhui.pathanimation.path.seekPath
import dev.wenhui.pathanimation.ui.theme.PathAnimationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PathAnimationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PathAnimationScreen()
                }
            }
        }
    }
}

@Composable
fun PathAnimationScreen(modifier: Modifier = Modifier) {
    var animationButtonEnabled by remember { mutableStateOf(true) }
    var sliderEnabled by remember { mutableStateOf(false) }
    var animationActive by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier.padding(16.dp)) {
        Row {
            AnimationRadioGroup(
                animationEnabled = animationButtonEnabled,
                sliderEnabled = sliderEnabled,
                onAnimationClick = {
                    animationButtonEnabled = true
                    sliderEnabled = false
                },
                onSliderClick = {
                    animationButtonEnabled = false
                    animationActive = false
                    sliderEnabled = true
                },
                modifier = Modifier.weight(1f)
            )
        }

        val segmentLength = 130.dp
        AnimatedImageVector(
            imageRes = R.drawable.spiral,
            pathRes = R.drawable.spiral_path,
            modifier = Modifier
                .align(Alignment.Center)
                .fixedHeightMatchRatio(200.dp)
                .conditional(animationButtonEnabled) {
                    animateImageVector(animationActive, segmentLength = segmentLength)
                }
                .conditional(sliderEnabled) {
                    seekPath(sliderValue, segmentLength = segmentLength)
                },
            pathStrokeWidth = 28.dp,
        )

        if (animationButtonEnabled) {
            Button(
                onClick = { animationActive = !animationActive },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp)
            ) {
                Text(text = if (animationActive) "Stop animation" else "Start animation")
            }
        }
        if (sliderEnabled) {
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp, start = 24.dp, end = 24.dp)
            )
        }
    }
}

@Composable
fun AnimationRadioGroup(
    animationEnabled: Boolean,
    sliderEnabled: Boolean,
    onAnimationClick: () -> Unit,
    onSliderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.selectableGroup()) {
        RadioTextButton(
            selected = animationEnabled,
            onClick = onAnimationClick,
            text = "Show animation button"
        )
        RadioTextButton(
            selected = sliderEnabled,
            onClick = onSliderClick,
            text = "Show seek path slider"
        )
    }
}

@Composable
private fun RadioTextButton(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}