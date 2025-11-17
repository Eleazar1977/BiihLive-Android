package com.mision.biihlive.data.perfil.mapper

import com.mision.biihlive.data.perfil.dto.PerfilUsuarioDto
import com.mision.biihlive.data.perfil.dto.UbicacionDto
import com.mision.biihlive.domain.perfil.model.PerfilUsuario
import com.mision.biihlive.domain.perfil.model.Ubicacion
import com.mision.biihlive.utils.Calcular
import com.mision.biihlive.utils.LevelCalculator

object PerfilMapper {
    
    private val calcular = Calcular()
    
    fun mapToDomain(dto: PerfilUsuarioDto): PerfilUsuario {
        val nivel = LevelCalculator.calculateLevel(dto.totalScore)
        
        return PerfilUsuario(
            userId = dto.userId,
            nickname = dto.nickname,
            fullName = dto.fullName,
            description = dto.description,
            totalScore = dto.totalScore,
            tipo = dto.tipo,
            ubicacion = Ubicacion(
                ciudad = dto.ubicacion.ciudad,
                provincia = dto.ubicacion.provincia,
                pais = dto.ubicacion.pais
            ),
            rankingPreference = dto.rankingPreference,
            createdAt = dto.createdAt,
            photoUrl = dto.photoUrl,
            email = dto.email,
            nivel = nivel,
            // Estos valores se pueden obtener de otras queries o mantener en caché
            seguidores = 0,
            siguiendo = 0,
            fotosCount = 0,
            videosCount = 0,
            rankingLocal = 0,
            rankingProvincial = 0,
            rankingNacional = 0,
            rankingMundial = 0
        )
    }
    
    fun mapToDto(perfil: PerfilUsuario): PerfilUsuarioDto {
        return PerfilUsuarioDto(
            PK = perfil.userId,
            SK = "PROFILE",
            userId = perfil.userId,
            nickname = perfil.nickname,
            fullName = perfil.fullName,
            description = perfil.description,
            totalScore = perfil.totalScore,
            tipo = perfil.tipo,
            ubicacion = perfil.ubicacion?.let { ubicacion ->
                UbicacionDto(
                    ciudad = ubicacion.ciudad,
                    provincia = ubicacion.provincia,
                    pais = ubicacion.pais
                )
            } ?: UbicacionDto(ciudad = "", provincia = "", pais = ""), // Provide default if null
            rankingPreference = perfil.rankingPreference,
            createdAt = perfil.createdAt,
            photoUrl = perfil.photoUrl,
            email = perfil.email
        )
    }
    
    fun mapFromDynamoDBItem(item: Map<String, Any>): PerfilUsuarioDto {
        val ubicacionMap = item["ubicacion"] as? Map<String, Any> ?: emptyMap()
        
        return PerfilUsuarioDto(
            PK = (item["PK"] as? Map<String, String>)?.get("S") ?: "",
            SK = (item["SK"] as? Map<String, String>)?.get("S") ?: "",
            userId = (item["userId"] as? Map<String, String>)?.get("S") ?: "",
            nickname = (item["nickname"] as? Map<String, String>)?.get("S") ?: "",
            fullName = (item["fullName"] as? Map<String, String>)?.get("S") ?: "",
            description = (item["description"] as? Map<String, String>)?.get("S") ?: "",
            totalScore = ((item["totalScore"] as? Map<String, String>)?.get("N") ?: "0").toIntOrNull() ?: 0,
            tipo = (item["tipo"] as? Map<String, String>)?.get("S") ?: "",
            ubicacion = UbicacionDto(
                ciudad = (ubicacionMap["ciudad"] as? Map<String, String>)?.get("S") ?: "",
                provincia = (ubicacionMap["provincia"] as? Map<String, String>)?.get("S") ?: "",
                pais = (ubicacionMap["pais"] as? Map<String, String>)?.get("S") ?: ""
            ),
            rankingPreference = (item["rankingPreference"] as? Map<String, String>)?.get("S") ?: "local",
            createdAt = ((item["createdAt"] as? Map<String, String>)?.get("N") ?: "0").toLongOrNull() ?: 0L,
            photoUrl = (item["photoUrl"] as? Map<String, String>)?.get("S"),
            email = (item["email"] as? Map<String, String>)?.get("S")
        )
    }
}