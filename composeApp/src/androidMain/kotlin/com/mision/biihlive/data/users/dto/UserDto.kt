package com.mision.biihlive.data.users.dto

import kotlinx.serialization.Serializable

/**
 * DTOs para usuarios desde DynamoDB
 */

@Serializable
data class UserDto(
    val userId: String,
    val nickname: String,
    val email: String? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val points: Int = 0,
    val level: Int = 1,
    val location: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null,
    val isVerified: Boolean = false,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val userType: String = "REGULAR"
)

@Serializable
data class UserPreviewDto(
    val userId: String,
    val nickname: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val userType: String = "REGULAR",
    val isVerified: Boolean = false,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0
)

@Serializable
data class UsersListResponse(
    val users: List<UserPreviewDto>
)