package presentation.components

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import presentation.viewmodels.SocialFeedViewModel
import utils.OptimizedImageLoader
import domain.models.Post
import com.mision.biihlive.R

/**
 * Feed de fotos social con puntuación y comentarios
 * Usa la nueva arquitectura con SocialFeedViewModel
 */
@Composable
fun SocialPhotoFeed(
    modifier: Modifier = Modifier,
    viewModel: SocialFeedViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    initialPhotoIndex: Int = 0,
    onNavigateToUserProfile: (String, Int) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()

    // FONDO NEGRO PARA CONTENIDO MULTIMEDIA
    val backgroundColor = Color.Black

    Box(modifier = modifier.fillMaxSize().background(backgroundColor)) {
        when {
            uiState.isLoading -> {
                LoadingIndicator()
            }

            uiState.error != null -> {
                ErrorScreen(
                    error = uiState.error!!,
                    onRetry = { viewModel.refreshFeed() }
                )
            }

            uiState.posts.isEmpty() -> {
                EmptyStateScreen()
            }

            else -> {
                SocialFeedContent(
                    posts = uiState.posts,
                    initialPhotoIndex = minOf(initialPhotoIndex, maxOf(0, uiState.posts.size - 1)),
                    onPageChanged = { index -> viewModel.updateCurrentPostIndex(index) },
                    onLikeClick = { post -> viewModel.givePoint(post) },
                    onCommentClick = { post -> viewModel.openComments(post) },
                    onShareClick = { post -> viewModel.sharePost(post) },
                    onNavigateToUserProfile = onNavigateToUserProfile
                )
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun ErrorScreen(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = error,
            color = Color.Red,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Text("Reintentar")
        }
    }
}

@Composable
private fun EmptyStateScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No hay posts disponibles",
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun SocialFeedContent(
    posts: List<Post>,
    initialPhotoIndex: Int,
    onPageChanged: (Int) -> Unit,
    onLikeClick: (Post) -> Unit,
    onCommentClick: (Post) -> Unit,
    onShareClick: (Post) -> Unit,
    onNavigateToUserProfile: (String, Int) -> Unit
) {
    val pagerState = rememberPagerState(
        pageCount = { posts.size },
        initialPage = initialPhotoIndex
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                Log.d("SocialPhotoFeed", "📄 Página actual: $page, total: ${posts.size}")
                onPageChanged(page)
            }
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        pageSpacing = 0.dp,
        userScrollEnabled = true
    ) { page ->
        val post = posts[page]

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Imagen principal con optimizaciones de rendimiento
            val startTime = remember { System.currentTimeMillis() }

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(post.mediaUrl)
                    .crossfade(true)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED) // ✅ Cache aggressive
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED) // ✅ Memory cache
                    .size(
                        width = OptimizedImageLoader.ImageSizes.FULL_IMAGE_WIDTH,
                        height = OptimizedImageLoader.ImageSizes.FULL_IMAGE_HEIGHT
                    )
                    .allowHardware(true) // ✅ Hardware acceleration
                    .placeholderMemoryCacheKey("post_${post.postId}") // ✅ Cache key
                    .memoryCacheKey("post_${post.postId}_full") // ✅ Memory key unique
                    .listener(
                        onStart = {
                            Log.d("ImageLoad", "🚀 [${post.postId}] Iniciando carga de imagen: ${post.mediaUrl}")
                        },
                        onSuccess = { _, _ ->
                            val loadTime = System.currentTimeMillis() - startTime
                            Log.d("ImageLoad", "✅ [${post.postId}] Imagen cargada exitosamente en ${loadTime}ms")
                        },
                        onError = { _, throwable ->
                            val loadTime = System.currentTimeMillis() - startTime
                            Log.e("ImageLoad", "❌ [${post.postId}] Error cargando imagen después de ${loadTime}ms", throwable.throwable)
                        }
                    )
                    .build(),
                imageLoader = OptimizedImageLoader.create(LocalContext.current),
                contentDescription = post.description.ifEmpty { "Biihlive Post" },
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Overlay social
            SocialMediaOverlay(
                post = post,
                onLikeClick = onLikeClick,
                onCommentClick = onCommentClick,
                onShareClick = onShareClick,
                onNavigateToUserProfile = onNavigateToUserProfile
            )
        }
    }
}

@Composable
private fun SocialMediaOverlay(
    post: Post,
    onLikeClick: (Post) -> Unit,
    onCommentClick: (Post) -> Unit,
    onShareClick: (Post) -> Unit,
    onNavigateToUserProfile: (String, Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Info del usuario (izquierda abajo)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
        ) {
            UserInfoSection(
                post = post,
                onNavigateToUserProfile = onNavigateToUserProfile
            )

            if (post.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = post.description,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Botones sociales (derecha)
        SocialActionsSection(
            post = post,
            onLikeClick = onLikeClick,
            onCommentClick = onCommentClick,
            onShareClick = onShareClick,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
private fun UserInfoSection(
    post: Post,
    onNavigateToUserProfile: (String, Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable {
            Log.d("SocialPhotoFeed", "👤 Navegando al perfil del usuario ${post.userId}")
            onNavigateToUserProfile(post.userId, 0)
        }
    ) {
        // Avatar del usuario
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(post.authorInfo?.profileImageUrl ?: generateDefaultProfileUrl(post.userId))
                .crossfade(true)
                .size(96, 96)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .memoryCacheKey("thumb_${post.userId}")
                .build(),
            contentDescription = "Avatar de ${post.authorInfo?.nickname ?: "Usuario"}",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            error = painterResource(id = R.drawable.ic_default_avatar),
            placeholder = painterResource(id = R.drawable.ic_default_avatar),
            fallback = painterResource(id = R.drawable.ic_default_avatar)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Nickname del usuario
        Text(
            text = post.authorInfo?.nickname ?: "Usuario",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Botón seguir (temporal)
        Button(
            onClick = {
                Log.d("SocialPhotoFeed", "👥 Follow/Unfollow usuario ${post.userId}")
            },
            modifier = Modifier.height(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Seguir",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SocialActionsSection(
    post: Post,
    onLikeClick: (Post) -> Unit,
    onCommentClick: (Post) -> Unit,
    onShareClick: (Post) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Botón de Puntuación en el CENTRO VERTICAL (como proyecto base)
        SocialActionWithAnimation(
            icon = ImageVector.vectorResource(id = R.drawable.puntuar),
            count = formatCount(post.likesCount),
            contentDescription = if (post.isLiked) "Ya puntuado" else "Puntuar",
            onClick = { onLikeClick(post) },
            iconSize = 40.dp, // Tamaño más grande como en PhotoFeed
            iconColor = if (post.isLiked) Color(0xFFFF6B35) else Color.White, // Naranja cuando está puntuado
            isActive = post.isLiked,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )

        // Comentarios y Compartir en la parte INFERIOR
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Botón de Comentarios
            SocialAction(
                icon = Icons.AutoMirrored.Filled.Comment,
                count = formatCount(post.commentsCount),
                contentDescription = "Comments",
                onClick = { onCommentClick(post) },
                iconSize = 28.dp
            )

            // Botón de Compartir
            SocialAction(
                icon = Icons.Filled.Share,
                count = formatCount(post.sharesCount),
                contentDescription = "Share",
                onClick = { onShareClick(post) },
                iconSize = 28.dp
            )
        }
    }
}

@Composable
private fun SocialAction(
    icon: ImageVector,
    count: String,
    contentDescription: String,
    onClick: () -> Unit,
    iconSize: Dp = 24.dp,
    iconColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = count,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun SocialActionWithAnimation(
    icon: ImageVector,
    count: String,
    contentDescription: String,
    onClick: () -> Unit,
    iconSize: Dp = 24.dp,
    iconColor: Color = Color.White,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.2f else 1.0f,
        label = "like_animation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier
                .size(iconSize)
                .scale(scale)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = count,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

// Funciones auxiliares
private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000 -> "${count / 1_000}K"
    else -> count.toString()
}

private fun generateDefaultProfileUrl(userId: String): String {
    return "https://d183hg75gdabnr.cloudfront.net/userprofile/$userId/thumbnail_1759240530172.png"
}

private fun generateOptimizedThumbnailUrl(userId: String): String {
    return "https://d183hg75gdabnr.cloudfront.net/userprofile/$userId/thumbnail_1759240530172.png"
}

private fun generateThumbnailUrl(fullUrl: String): String {
    return fullUrl.replace("/full_", "/thumbnail_")
}