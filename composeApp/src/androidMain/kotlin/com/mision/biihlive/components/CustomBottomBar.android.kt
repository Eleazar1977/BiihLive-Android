package com.mision.biihlive.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mision.biihlive.R
import com.mision.biihlive.ui.theme.Gray500
import com.mision.biihlive.ui.theme.Gray600
import org.jetbrains.compose.resources.stringResource
import biihlive.composeapp.generated.resources.Res
import biihlive.composeapp.generated.resources.*

sealed class BottomNavItem(
    val route: String,
    val iconRes: Int,
    val labelResId: String
) {
    object Home : BottomNavItem("home", R.drawable.home_icon, "home")
    object Events : BottomNavItem("events", R.drawable.events_icon, "events")
    object Live : BottomNavItem("live", R.drawable.logo_n, "live")
    object Messages : BottomNavItem("messages", R.drawable.message_icon, "messages")
    object Profile : BottomNavItem("profile", R.drawable.profile_icon, "profile")
}

@Composable
fun CustomBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    unreadMessages: Int = 0,
    modifier: Modifier = Modifier
) {
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Events,
        BottomNavItem.Live,
        BottomNavItem.Messages,
        BottomNavItem.Profile
    )

    Column(modifier = modifier) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(64.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                bottomNavItems.forEach { item ->
                    BottomBarItem(
                        item = item,
                        selected = currentRoute == item.route,
                        unreadCount = if (item is BottomNavItem.Messages) unreadMessages else 0,
                        onClick = { onNavigate(item.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    item: BottomNavItem,
    selected: Boolean,
    unreadCount: Int = 0,
    onClick: () -> Unit
) {
    val label = when(item.labelResId) {
        "home" -> stringResource(Res.string.home)
        "events" -> stringResource(Res.string.events)
        "live" -> stringResource(Res.string.live)
        "messages" -> stringResource(Res.string.messages)
        "profile" -> stringResource(Res.string.profile)
        else -> item.labelResId
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .clickable(onClick = onClick)
            .height(60.dp)
    ) {
        Box {
            Icon(
                imageVector = ImageVector.vectorResource(item.iconRes),
                contentDescription = label,
                tint = if (item is BottomNavItem.Live) com.mision.biihlive.ui.theme.BiihliveOrangeLight
                      else if (selected) Gray600
                      else Gray500,
                modifier = Modifier.size(26.dp)
            )
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .background(
                            color = Color.Red,
                            shape = RoundedCornerShape(9.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (item is BottomNavItem.Live) com.mision.biihlive.ui.theme.BiihliveOrangeLight
                   else if (selected) Gray600
                   else Gray500,
            textAlign = TextAlign.Center
        )
    }
}