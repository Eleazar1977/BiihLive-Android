package com.mision.biihlive.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mision.biihlive.R
import com.mision.biihlive.ui.theme.BiihliveOrangeLight
import com.mision.biihlive.ui.theme.Gray500
import com.mision.biihlive.ui.theme.Gray600
import com.mision.biihlive.presentation.chat.providers.useGlobalChat
import org.jetbrains.compose.resources.stringResource
import biihlive.composeapp.generated.resources.Res
import biihlive.composeapp.generated.resources.*

sealed class NavigationItem(
    val route: String,
    val iconRes: Int,
    val labelResId: String
) {
    object Home : NavigationItem("home", R.drawable.home_icon, "home")
    object Events : NavigationItem("events", R.drawable.events_icon, "events")
    object Live : NavigationItem("live", R.drawable.logo_n, "live")
    object Messages : NavigationItem("messages", R.drawable.message_icon, "messages")
    object Profile : NavigationItem("profile", R.drawable.profile_icon, "profile")
}

@Composable
fun BiihliveNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Obtener el conteo de mensajes no leídos desde el provider global
    val globalChatViewModel = useGlobalChat()
    val unreadMessages by (globalChatViewModel?.unreadCount?.collectAsState(initial = 0) ?: run {
        return@run kotlinx.coroutines.flow.flowOf(0).collectAsState(initial = 0)
    })
    val navigationItems = listOf(
        NavigationItem.Home,
        NavigationItem.Events,
        NavigationItem.Live,
        NavigationItem.Messages,
        NavigationItem.Profile
    )

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Línea delimitadora superior
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )

        NavigationBar(
            modifier = Modifier
                .fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp
        ) {
            Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            navigationItems.forEach { item ->
                val isSelected = currentRoute == item.route
                val isLive = item is NavigationItem.Live

                val iconColor = when {
                    isLive -> BiihliveOrangeLight
                    isSelected -> Gray600
                    else -> Gray500
                }

                val textColor = when {
                    isLive -> BiihliveOrangeLight
                    isSelected -> Gray600
                    else -> Gray500
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(item.route) }
                        .padding(top = 0.5.dp, bottom = 1.5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    BadgedBox(
                        badge = {
                            if (item is NavigationItem.Messages && unreadMessages > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (unreadMessages > 99) "99+" else unreadMessages.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(item.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(if (isLive) 28.dp else 24.dp),
                            tint = iconColor
                        )
                    }

                    Spacer(modifier = Modifier.height(0.5.dp))

                    val label = when(item.labelResId) {
                        "home" -> stringResource(Res.string.home)
                        "events" -> stringResource(Res.string.events)
                        "live" -> stringResource(Res.string.live)
                        "messages" -> stringResource(Res.string.messages)
                        "profile" -> stringResource(Res.string.profile)
                        else -> item.labelResId
                    }

                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = textColor
                    )
                }
            }
        }
    }
    }
}
