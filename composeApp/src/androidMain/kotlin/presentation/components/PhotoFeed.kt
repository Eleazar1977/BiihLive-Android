package presentation.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import presentation.viewmodels.SimplePhotoViewModel

@Composable
fun PhotoFeed(
    modifier: Modifier = Modifier,
    viewModel: SimplePhotoViewModel? = null
) {
    val context = LocalContext.current
    val photoViewModel = remember { viewModel ?: SimplePhotoViewModel(context) }
    val uiState by photoViewModel.uiState.collectAsState()

    // FONDO NEGRO PARA AMBOS TEMAS (CONTENIDO MULTIMEDIA)
    val backgroundColor = Color.Black

    Box(modifier = modifier.fillMaxSize().background(backgroundColor)) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            
            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.error!!,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { photoViewModel.refreshPhotos() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Reintentar")
                    }
                }
            }
            
            uiState.photos.isEmpty() -> {
                Text(
                    text = "No hay fotos disponibles",
                    color = Color.White.copy(alpha = 0.8f), // Texto claro para fondo negro
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            else -> {
                val pagerState = rememberPagerState(pageCount = { uiState.photos.size })
                
                // Track current photo for pagination
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }
                        .collect { page ->
                            Log.d("PhotoFeed", "Current page: $page, total: ${uiState.photos.size}")
                            photoViewModel.updateCurrentPhotoIndex(page)
                        }
                }
                
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = 0.dp,
                    userScrollEnabled = true
                ) { page ->
                    val photo = uiState.photos[page]
                    
                    // Center the photo properly within the screen bounds
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor), // Asegurar el fondo correcto para cada imagen
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(photo.photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = photo.description.ifEmpty { "Biihlive Photo" },
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}