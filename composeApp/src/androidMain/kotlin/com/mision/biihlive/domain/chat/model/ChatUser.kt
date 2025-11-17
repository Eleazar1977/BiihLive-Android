package com.mision.biihlive.domain.chat.model

/**
 * Representa la información de un usuario en el contexto del chat
 */
data class ChatUser(
    val userId: String,
    val nickname: String,
    val imageUrl: String? = null,
    val lastSeen: Long? = null,
    val userType: UserType = UserType.REGULAR
)

enum class UserType {
    REGULAR,
    SUBSCRIBER,
    FOLLOWER,
    FOLLOWING,
    VERIFIED,
    PREMIUM
}