package com.mision.biihlive.presentation.ranking.viewmodel

/**
 * Estado de la UI para el sistema de ranking
 */
data class RankingUiState(
    val currentTab: Int = 0,
    val error: String? = null,
    val localRanking: List<RankingUser> = emptyList(),
    val provincialRanking: List<RankingUser> = emptyList(),
    val nacionalRanking: List<RankingUser> = emptyList(),
    val mundialRanking: List<RankingUser> = emptyList(),
    val grupoRanking: List<RankingUser> = emptyList(),
    val loadingTabs: Set<Int> = emptySet()
)