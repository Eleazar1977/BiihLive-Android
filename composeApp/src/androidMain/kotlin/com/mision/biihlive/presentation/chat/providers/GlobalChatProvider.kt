package com.mision.biihlive.presentation.chat.providers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mision.biihlive.presentation.chat.viewmodel.GlobalChatViewModel

/**
 * CompositionLocal para proporcionar el GlobalChatViewModel a toda la app
 */
val LocalGlobalChatViewModel = compositionLocalOf<GlobalChatViewModel?> { null }

/**
 * Provider que proporciona el GlobalChatViewModel a toda la jerarquía de composables
 */
@Composable
fun GlobalChatProvider(
    context: Context,
    content: @Composable () -> Unit
) {
    val globalChatViewModel: GlobalChatViewModel = viewModel {
        GlobalChatViewModel(context)
    }

    CompositionLocalProvider(
        LocalGlobalChatViewModel provides globalChatViewModel
    ) {
        content()
    }
}

/**
 * Hook para obtener el GlobalChatViewModel desde cualquier composable
 */
@Composable
fun useGlobalChat(): GlobalChatViewModel? {
    return LocalGlobalChatViewModel.current
}