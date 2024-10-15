@file:OptIn(ExperimentalAnimationApi::class)

package com.example.storypagercompose

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.example.storypagercompose.ui.theme.StoryPagerComposeTheme
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StoryPagerComposeTheme {
                WhatsAppStyleStoryPager()

            }
        }
    }
}

@Composable
fun WhatsAppStyleStoryPager() {
    val stepCount = stories().size
    val currentStep = remember { mutableIntStateOf(0) }
    val isPaused = remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val storyList = stories()
    val context = LocalContext.current
    val bitmap = BitmapFactory.decodeResource(context.resources, storyList[currentStep.intValue])
    var dominantColor by remember { mutableStateOf(White) }
    var progressColor by remember { mutableStateOf(White) }

    LaunchedEffect(bitmap) {
        val palette = Palette.from(bitmap).generate()
        dominantColor = Color(palette.getDarkMutedColor(White.hashCode()))
        progressColor = Color(palette.getLightMutedColor(White.hashCode()))
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize().background(color = dominantColor)
            .padding(top = 80.dp)
    ) {
        val parentModifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        try {
                            isPaused.value = true
                            awaitRelease()
                        } finally {
                            isPaused.value = false
                        }
                    },
                    onTap = { offset ->
                        val screenWidth = constraints.maxWidth
                        if (offset.x < screenWidth * 0.25f) {
                            haptic.performHapticFeedback(hapticFeedbackType = HapticFeedbackType.LongPress)
                            currentStep.intValue = max(0, currentStep.intValue - 1)
                        } else if (offset.x > screenWidth * 0.60f) {
                            haptic.performHapticFeedback(hapticFeedbackType = HapticFeedbackType.LongPress)
                            currentStep.intValue = min(stepCount - 1, currentStep.intValue + 1)
                        }
                    }
                )
            }

        Column(modifier = parentModifier) {
            ProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                stepCount = stepCount,
                stepDuration = 8_000,
                unSelectedColor = progressColor.copy(alpha = .1f),
                selectedColor = progressColor,
                currentStep = currentStep.intValue,
                onStepChanged = { currentStep.intValue = it },
                isPaused = isPaused.value,
                onComplete = { }
            )

            AnimatedContent(
                targetState = currentStep.intValue,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() with
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() with
                                slideOutHorizontally { width -> width } + fadeOut()
                    }.using(SizeTransform(clip = false))
                }, label = ""
            ) { targetStep ->
                LoadImageUrl(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp)),
                    imageUrl = storyList[targetStep]
                )
            }
        }
    }
}

@Composable
fun ProgressIndicator(
    modifier: Modifier = Modifier,
    stepCount: Int,
    stepDuration: Int,
    unSelectedColor: Color,
    selectedColor: Color,
    currentStep: Int,
    onStepChanged: (Int) -> Unit,
    isPaused: Boolean = false,
    onComplete: () -> Unit,
) {
    val currentStepState = remember(currentStep) { mutableIntStateOf(currentStep) }
    val progress = remember { Animatable(0f) }
    var remainingTime = remember { stepDuration }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(currentStep) {
        currentStepState.intValue = currentStep
        remainingTime = stepDuration
        progress.snapTo(0f)
        if (!isPaused) {
            coroutineScope.launch {
                progress.animateTo(
                    1f,
                    animationSpec = tween(
                        durationMillis = stepDuration,
                        easing = LinearEasing
                    )
                )
                if (currentStepState.intValue < stepCount - 1) {
                    currentStepState.intValue++
                    onStepChanged(currentStepState.intValue)
                    remainingTime = stepDuration
                    progress.snapTo(0f)
                } else {
                    onComplete()
                }
            }
        }
    }

    Row(
        modifier = modifier
    ) {
        for (i in 0 until stepCount) {
            val stepProgress = when {
                i == currentStepState.intValue -> progress.value
                i > currentStepState.intValue -> 0f
                else -> 1f
            }
            LinearProgressIndicator(
                progress = stepProgress,
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .height(10.dp),
                color = selectedColor,
                trackColor = unSelectedColor,
            )
        }
    }

    LaunchedEffect(isPaused) {
        if (isPaused) {
            remainingTime = (stepDuration * (1f - progress.value)).toInt()
            progress.stop()
        } else {
            val duration =
                (remainingTime * (1f - progress.value)).toInt()
            coroutineScope.launch {
                progress.animateTo(
                    1f,
                    animationSpec = tween(
                        durationMillis = duration,
                        easing = LinearEasing
                    )
                )
                if (currentStepState.intValue < stepCount - 1) {
                    currentStepState.intValue++
                    onStepChanged(currentStepState.intValue)
                    remainingTime = stepDuration
                    progress.snapTo(0f)
                } else {
                    onComplete()
                }
            }
        }
    }
}

fun stories() = listOf(
    R.drawable.minion,
    R.drawable.pink_panther,
    R.drawable.monkey,
    R.drawable.bunny,
    R.drawable.pichu,
)

fun createPaletteSync(bitmap: Bitmap): Palette = Palette.from(bitmap).generate()
