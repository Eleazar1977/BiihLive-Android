package utils

/**
 * Feature flags para rollout gradual de optimizaciones de imágenes
 * 
 * FASE 1: Todos en false (preparación)
 * FASE 2: Generación de metadata para nuevos uploads
 * FASE 3: Activación gradual de optimizaciones UI
 * FASE 4: Rollout completo al 100%
 */
object FeatureFlags {
    
    // ========== OPTIMIZACIONES DE IMÁGENES ==========
    
    /**
     * FASE 3 - Día 1-2
     * Mostrar placeholders con BlurHash mientras carga la imagen real
     * Mejora: Placeholder instantáneo (<50ms) vs pantalla blanca
     */
    const val USE_BLURHASH_PLACEHOLDERS = false
    
    /**
     * FASE 3 - Día 5-7
     * Feed optimizado: mezcla 80% posts recientes + 20% aleatorios antiguos
     * Mejora: Mayor variedad vs solo cronológico
     */
    const val USE_OPTIMIZED_FEED = false
    
    /**
     * FASE 3 - Día 3-4
     * Priorizar carga de primera imagen del feed (Priority.HIGH)
     * Mejora: Primera imagen carga 60% más rápido
     */
    const val USE_PRIORITY_LOADING = false
    
    /**
     * FASE 4 - Rollout final
     * URLs optimizadas con CloudFront transformations (AVIF, WebP, resize)
     * Requiere: Lambda@Edge configurado en CloudFront
     * Mejora: -50% a -70% en data transferido
     */
    const val USE_CLOUDFRONT_TRANSFORMS = false
    
    // ========== CONFIGURACIÓN DE IMÁGENES ==========
    
    object ImageConfig {
        // Tamaños target según viewport del dispositivo
        const val MOBILE_WIDTH = 640      // Pantallas < 400dp
        const val TABLET_WIDTH = 1024     // Pantallas 400-800dp
        const val DESKTOP_WIDTH = 1920    // Pantallas > 800dp
        
        // Calidad JPEG/WebP (0-100)
        const val DEFAULT_QUALITY = 85    // Balance calidad/tamaño
        const val THUMBNAIL_QUALITY = 75  // Para thumbnails pequeños
        
        // Formato preferido (CloudFront auto-detecta soporte AVIF/WebP)
        const val PREFERRED_FORMAT = "auto"
        
        // Componentes BlurHash (más = más detalle pero más bytes)
        const val BLURHASH_COMPONENT_X = 4
        const val BLURHASH_COMPONENT_Y = 3
        
        // Cache configuration
        const val MEMORY_CACHE_PERCENT = 0.25  // 25% RAM para imágenes
        const val DISK_CACHE_MB = 200          // 200MB en disco
    }
    
    // ========== CONFIGURACIÓN DE FEEDS ==========
    
    object FeedConfig {
        // Proporción de posts en feed optimizado
        const val RECENT_POSTS_PERCENT = 0.80  // 80% posts recientes
        const val RANDOM_POSTS_PERCENT = 0.20  // 20% posts aleatorios
        
        // Tamaño de página para paginación
        const val DEFAULT_PAGE_SIZE = 25
        
        // Cantidad de posts a cargar de más para compensar filtrado
        const val FETCH_MULTIPLIER = 2
    }
    
    // ========== DEBUG ==========
    
    /**
     * Habilitar logs detallados de performance de imágenes
     * Solo para desarrollo, desactivar en producción
     */
    const val DEBUG_IMAGE_PERFORMANCE = false
}
