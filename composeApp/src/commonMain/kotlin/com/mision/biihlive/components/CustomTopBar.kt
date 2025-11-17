package com.mision.biihlive.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
fun HomeTopBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onRankingClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding() // Agregar padding para evitar solapamiento con status bar
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono izquierdo - Ranking
            IconButton(onClick = onRankingClick) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.rank),
                    contentDescription = "Ranking",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(35.dp)
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
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(25.dp)
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
        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardTopBar(
    title: String,
    onBackClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier
    )
}