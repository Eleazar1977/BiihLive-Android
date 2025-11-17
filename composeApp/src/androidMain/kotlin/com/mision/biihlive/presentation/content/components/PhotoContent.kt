package com.mision.biihlive.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import presentation.components.SocialPhotoFeed
import presentation.viewmodels.SocialFeedViewModel

@Composable
fun PhotoContent(
    modifier: Modifier = Modifier,
    onNavigateToUserProfile: (String, Int) -> Unit = { _, _ -> }
) {
    // Usar el nuevo feed social con likes y comentarios
    val socialFeedViewModel: SocialFeedViewModel = viewModel()

    val density = LocalDensity.current

    // Obtener el padding dinámico del navbar desde WindowInsets
    val navigationBarPadding = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }

    // Padding adicional para el navbar custom de la app (estimado)
    val customNavBarHeight = 80.dp // Altura estimada del BiihliveNavigationBar
    val totalBottomPadding = navigationBarPadding + customNavBarHeight

    SocialPhotoFeed(
        modifier = modifier
            .padding(top = 56.dp, bottom = totalBottomPadding),
        viewModel = socialFeedViewModel,
        onNavigateToUserProfile = onNavigateToUserProfile
    )
}