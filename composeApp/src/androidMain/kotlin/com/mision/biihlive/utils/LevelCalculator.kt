package com.mision.biihlive.utils

import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.pow

/**
 * Calculadora de niveles basada en el sistema de puntos del usuario.
 *
 * Implementa un sistema dinámico donde cada nivel requiere progresivamente
 * más puntos, utilizando tasas de crecimiento exponenciales controladas.
 */
object LevelCalculator {

    /**
     * Calcula el nivel del usuario basado en sus puntos totales
     *
     * @param points Puntos totales del usuario
     * @return Nivel calculado (mínimo 1)
     */
    fun calculateLevel(points: Int): Int {
        var currentThreshold = 100
        var level = 1
        var currentPoints = points

        while (currentPoints > currentThreshold) {
            val increaseRate = calculateDynamicIncreaseRate(currentThreshold)
            val increase = (currentThreshold * increaseRate).toInt()
            currentThreshold += increase
            level++
        }

        return level
    }

    /**
     * Calcula la tasa de crecimiento dinámico para el siguiente umbral
     *
     * @param points Puntos actuales
     * @return Tasa de crecimiento (entre 0.0 y 1.0)
     */
    private fun calculateDynamicIncreaseRate(points: Int): Double {
        // Comenzamos con 100% (1.0) para puntos <= 200
        val maxRate = 1.0

        if (points <= 200) return maxRate

        // Utilizamos log10 para manejar grandes números de manera más efectiva
        val logPoints = log10(points.toDouble())

        // Factor de escala para controlar qué tan rápido disminuye la tasa
        // Mientras más grande sea este número, más gradual será la disminución
        val scaleFactor = 2.0

        // Calculamos la tasa usando una función exponencial inversa pura
        return maxRate * exp(-logPoints / scaleFactor)
    }

    /**
     * Obtiene el umbral de puntos necesarios para el siguiente nivel
     *
     * @param points Puntos actuales del usuario
     * @return Puntos necesarios para el siguiente nivel
     */
    fun getThresholdForNextLevel(points: Int): Int {
        var threshold = 200
        var currentPoints = points

        while (currentPoints > threshold) {
            val increaseRate = calculateDynamicIncreaseRate(threshold)
            val increase = (threshold * increaseRate).toInt()
            threshold += increase
        }

        return threshold
    }

    /**
     * Calcula el progreso hacia el siguiente nivel como porcentaje
     *
     * @param points Puntos actuales del usuario
     * @return Progreso de 0.0 a 1.0 (0% a 100%)
     */
    fun calculateProgressToNextLevel(points: Int): Double {
        val currentThreshold = getThresholdForNextLevel(points)
        val previousThreshold = if (points <= 200) 0 else {
            var threshold = 200
            var prevThreshold = 0

            while (points > threshold) {
                prevThreshold = threshold
                threshold += (threshold * 0.5).toInt()
            }
            prevThreshold
        }

        val progressPoints = points - previousThreshold
        val levelRange = currentThreshold - previousThreshold

        return if (levelRange > 0) {
            (progressPoints.toDouble() / levelRange).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
    }

    /**
     * Obtiene información completa del nivel del usuario
     *
     * @param points Puntos totales del usuario
     * @return Información del nivel incluyendo nivel actual, progreso y próximo umbral
     */
    fun getLevelInfo(points: Int): LevelInfo {
        val currentLevel = calculateLevel(points)
        val nextThreshold = getThresholdForNextLevel(points)
        val progress = calculateProgressToNextLevel(points)

        return LevelInfo(
            currentLevel = currentLevel,
            currentPoints = points,
            nextLevelThreshold = nextThreshold,
            progressToNext = progress
        )
    }

    /**
     * Información completa del nivel de un usuario
     */
    data class LevelInfo(
        val currentLevel: Int,
        val currentPoints: Int,
        val nextLevelThreshold: Int,
        val progressToNext: Double
    ) {
        /**
         * Puntos restantes para el siguiente nivel
         */
        val pointsToNext: Int
            get() = nextLevelThreshold - currentPoints

        /**
         * Progreso como porcentaje (0-100)
         */
        val progressPercentage: Int
            get() = (progressToNext * 100).toInt()
    }
}