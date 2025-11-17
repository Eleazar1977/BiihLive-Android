package com.mision.biihlive.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mision.biihlive.R

@Composable
fun HomeTopBarWithProfile(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onRankingClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(Color.Black)
    ) {
        // Top row with profile
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App name or logo
            Text(
                text = "Biihlive",
                color = Color(0xFFFF7300),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            // Google Profile
            GoogleProfileDisplay(
                onProfileClick = onProfileClick
            )
        }
        
        // Original top bar with tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono izquierdo - Ranking
            IconButton(onClick = onRankingClick) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.rank),
                    contentDescription = "Ranking",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Tabs en el centro
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabOption("VIVOS", selected = selectedTab == 0) { onTabSelected(0) }
                TabOption("VIDEOS", selected = selectedTab == 1) { onTabSelected(1) }
                TabOption("FOTOS", selected = selectedTab == 2) { onTabSelected(2) }
            }

            // Icono derecho - Búsqueda
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.busqueda),
                    contentDescription = "Buscar",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun TabOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
    )
}