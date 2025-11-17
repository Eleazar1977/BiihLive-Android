package com.mision.biihlive.config

/**
 * Configuración de servicios AWS para Biihlive
 *
 * IMPORTANTE: Después de ejecutar los comandos de AWS CLI para crear
 * la infraestructura de invalidación de CloudFront, actualizar el API_GATEWAY_ID
 * con el valor real obtenido del deploy.
 */
object AWSConfig {
    // Región principal de AWS
    const val REGION = "eu-west-3"

    // CloudFront
    const val CLOUDFRONT_DISTRIBUTION_ID = "E1HZ8WQ7IXAQXD"
    const val CLOUDFRONT_DOMAIN = "d183hg75gdabnr.cloudfront.net"

    // S3
    const val S3_BUCKET = "biihlivemedia"

    // Cognito Identity Pool - SOLO para credenciales temporales de S3 (no para autenticación de usuario)
    // La autenticación de usuario se hace con Firebase Auth
    const val IDENTITY_POOL_ID = "eu-west-3:93df5af8-4cf5-4520-b868-cb586153655f"

    // API Gateway para invalidación de CloudFront
    // Desplegado el 2025-09-30
    const val API_GATEWAY_ID = "ig0ikgy5df"

    /**
     * Obtiene el endpoint de invalidación de CloudFront
     */
    fun getInvalidationEndpoint(): String {
        return "https://$API_GATEWAY_ID.execute-api.$REGION.amazonaws.com/prod/invalidate"
    }

    /**
     * Verifica si la configuración de API Gateway está lista
     */
    fun isApiGatewayConfigured(): Boolean {
        return API_GATEWAY_ID != "YOUR_API_ID"
    }
}