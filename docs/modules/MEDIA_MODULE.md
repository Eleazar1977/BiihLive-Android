# 📹 Módulo Multimedia

## Estado: 🚧 En Desarrollo

## Componentes

### ViewModels
- `VideoPlayerViewModel.kt` - Reproductor de video
- `SimplePhotoViewModel.kt` - Visor de fotos
- `LiveStreamViewModel.kt` - Streaming en vivo (pendiente)

### Screens
- `VideoFeed.kt` - Feed estilo TikTok
- `PhotoFeed.kt` - Galería de fotos
- `LiveStreamScreen.kt` - Transmisión en vivo (pendiente)

## Sistema de Videos

### Estructura S3
```
videos/
└── {userId}/
    ├── original/
    │   └── video_{timestamp}.mp4
    ├── transcoded/
    │   ├── video_{timestamp}_720p.mp4
    │   ├── video_{timestamp}_480p.mp4
    │   └── video_{timestamp}_360p.mp4
    └── thumbnails/
        └── thumb_{timestamp}.jpg
```

### ExoPlayer Setup
```kotlin
@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
            }
    }

    DisposableEffect(
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = modifier.fillMaxSize()
        )
    ) {
        onDispose {
            exoPlayer.release()
        }
    }
}
```

### Feed Vertical (TikTok-style)
```kotlin
@Composable
fun VideoFeed(
    videos: List<Video>,
    currentIndex: Int,
    onSwipe: (Int) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { videos.size })

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            VideoPlayer(
                videoUrl = videos[page].url,
                isPlaying = page == pagerState.currentPage
            )

            // Overlay con info
            VideoOverlay(
                video = videos[page],
                modifier = Modifier.align(Alignment.BottomStart)
            )

            // Controles
            VideoControls(
                onLike = { },
                onComment = { },
                onShare = { },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}
```

## Sistema de Fotos

### Upload Flow
```kotlin
class PhotoUploadManager {
    suspend fun uploadPhoto(
        uri: Uri,
        userId: String,
        caption: String? = null
    ): Result<Photo> {
        // 1. Comprimir
        val compressed = ImageProcessor.compress(
            uri = uri,
            maxWidth = 1920,
            maxHeight = 1920,
            quality = 85
        )

        // 2. Generar thumbnail
        val thumbnail = ImageProcessor.createThumbnail(
            uri = uri,
            size = 300,
            quality = 70
        )

        // 3. Upload a S3
        val timestamp = System.currentTimeMillis()
        val photoKey = "fotos/$userId/full_$timestamp.jpg"
        val thumbKey = "fotos/$userId/thumb_$timestamp.jpg"

        S3Client.upload(photoKey, compressed)
        S3Client.upload(thumbKey, thumbnail)

        // 4. Guardar en DB
        return appSyncRepository.createPhoto(
            userId = userId,
            photoUrl = photoKey,
            thumbnailUrl = thumbKey,
            caption = caption
        )
    }
}
```

### Grid de Galería
```kotlin
@Composable
fun PhotoGrid(
    photos: List<Photo>,
    onPhotoClick: (Photo) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(photos, key = { it.id }) { photo ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("${CloudFront.URL}/${photo.thumbnailUrl}")
                    .crossfade(true)
                    .memoryCacheKey("photo_${photo.id}")
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable { onPhotoClick(photo) },
                contentScale = ContentScale.Crop
            )
        }
    }
}
```

## Live Streaming (Pendiente)

### Arquitectura Planeada
```
Mobile → RTMP → Media Live → Media Package → CloudFront → Viewers
                     ↓
                Media Store (VOD)
```

### Componentes AWS
- **Elemental MediaLive**: Encoding
- **Elemental MediaPackage**: Packaging HLS
- **CloudFront**: Distribution CDN
- **S3**: VOD storage

### UI Broadcaster
```kotlin
@Composable
fun LiveBroadcastScreen(
    onStartStream: () -> Unit,
    onEndStream: () -> Unit
) {
    var isStreaming by remember { mutableStateOf(false) }
    var viewerCount by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        CameraPreview(
            modifier = Modifier.fillMaxSize()
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Live indicator
            if (isStreaming) {
                LiveIndicator(viewerCount = viewerCount)
            }

            // Close button
            IconButton(onClick = onEndStream) {
                Icon(Icons.Default.Close, contentDescription = null)
            }
        }

        // Bottom controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        ) {
            // Start/Stop button
            FloatingActionButton(
                onClick = {
                    if (isStreaming) onEndStream() else onStartStream()
                    isStreaming = !isStreaming
                },
                containerColor = if (isStreaming) Color.Red else BiihliveOrangeLight
            ) {
                Icon(
                    if (isStreaming) Icons.Default.Stop else Icons.Default.Videocam,
                    contentDescription = null
                )
            }
        }
    }
}
```

### Viewer UI
```kotlin
@Composable
fun LiveViewerScreen(
    streamUrl: String,
    streamerId: String
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // HLS Player
        HLSVideoPlayer(
            url = streamUrl,
            modifier = Modifier.fillMaxSize()
        )

        // Chat overlay
        LiveChat(
            streamId = streamerId,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.3f)
                .align(Alignment.BottomStart)
        )

        // Reactions
        ReactionButtons(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}
```

## Optimizaciones

### Caché de Imágenes
```kotlin
// Coil configuration
ImageLoader.Builder(context)
    .memoryCache {
        MemoryCache.Builder(context)
            .maxSizePercent(0.25) // 25% RAM
            .build()
    }
    .diskCache {
        DiskCache.Builder()
            .directory(context.cacheDir.resolve("image_cache"))
            .maxSizeBytes(100L * 1024 * 1024) // 100MB
            .build()
    }
    .respectCacheHeaders(false) // Ignorar cache headers del servidor
    .build()
```

### Video Buffering
```kotlin
val loadControl = DefaultLoadControl.Builder()
    .setBufferDurationsMs(
        MIN_BUFFER_MS,      // 15 seconds
        MAX_BUFFER_MS,      // 30 seconds
        PLAYBACK_START_MS,  // 2.5 seconds
        PLAYBACK_REBUFFER_MS // 5 seconds
    )
    .build()
```

### Lazy Loading
```kotlin
// Cargar solo visibles + 2 adelante
LazyColumn {
    items(
        items = mediaItems,
        key = { it.id }
    ) { item ->
        val index = mediaItems.indexOf(item)
        val shouldLoad = abs(currentIndex - index) <= 2

        if (shouldLoad) {
            MediaItem(item)
        } else {
            MediaPlaceholder()
        }
    }
}
```

## Métricas y Analytics

### Video Metrics
- Views count
- Watch time (segundos vistos)
- Completion rate
- Engagement (likes/comments por view)

### Photo Metrics
- Views
- Likes
- Downloads
- Share count

## Próximas Features
- [ ] Filtros y efectos (tipo Instagram)
- [ ] Editor de video básico
- [ ] Stickers y texto en stories
- [ ] Música de fondo
- [ ] Duetos (tipo TikTok)
- [ ] Transmisión con invitados
- [ ] Monetización (gifts/donations)