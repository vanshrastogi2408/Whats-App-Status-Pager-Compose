A fully fledged jetpack compose developed pager for Whatsapp status, Facbook Highlights or stories features of instagram and other platforms.

https://github.com/user-attachments/assets/4a63ae49-4f61-4460-bfc4-ae140dce9056

This can be used in compose multiplatform also by using the below functions

@Composable
fun WhatsAppStyleStoryPager() {
    val stepCount = stories().size
    val currentStep = remember { mutableIntStateOf(0) }
    val isPaused = remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val storyList = stories()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.sdp)
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
                stepDuration = 5_000,
                unSelectedColor = White.copy(alpha = 0.1f),
                selectedColor = White,
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
                val newsImage = asyncPainterResource(storyList[targetStep].image)
                KamelImage(
                    modifier = Modifier
                        .height(500.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    resource = newsImage,
                    contentScale = ContentScale.Crop,
                    contentDescription = null
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


    Row(modifier = modifier) {
        for (i in 0 until stepCount) {
            val stepProgress = when {
                i == currentStepState.value -> progress.value
                i > currentStepState.value -> 0f
                else -> 1f
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .padding(2.sdp)
                    .height(2.sdp)
            ) {
                drawRect(
                    color = unSelectedColor,
                    size = size.copy(width = size.width)
                )
                drawRect(
                    color = selectedColor,
                    size = size.copy(width = size.width * stepProgress)
                )
            }
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
