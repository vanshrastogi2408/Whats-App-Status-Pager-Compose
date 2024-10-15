package com.example.storypagercompose

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

@Composable
fun LoadImageUrl(modifier: Modifier, imageUrl: Any) {
    val scale = remember { Animatable(1.25f) }

    LaunchedEffect(imageUrl) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(8000, easing = LinearEasing)
        )
    }

    Crossfade(targetState = imageUrl, label = "") { imageUrl ->
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .error(R.drawable.image_place_holder)
                .placeholder(R.drawable.image_place_holder)
                .crossfade(true)
                .build(),
            loading = {
                Box(modifier = Modifier.fillMaxSize().shimmerEffect())
            },
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Crop,
            modifier = modifier.scale(scale.value)
        )
    }
}