package com.mision.biihlive.utils

class Calcular {

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

    fun calculateDynamicIncreaseRate(points: Int): Double {
        // Comenzamos con 50% (0.5) para puntos <= 100
        val maxRate = 1.0

        if (points <= 200) return maxRate

        // Utilizamos log10 para manejar grandes números de manera más efectiva
        val logPoints = kotlin.math.log10(points.toDouble())

        // Factor de escala para controlar qué tan rápido disminuye la tasa
        // Mientras más grande sea este número, más gradual será la disminución
        val scaleFactor = 2.0

        // Calculamos la tasa usando una función exponencial inversa pura
        return maxRate * kotlin.math.exp(-logPoints / scaleFactor)
    }

    fun getThresholdForPoints(points: Int): Int {
        var threshold = 200
        var currentPoints = points

        while (currentPoints > threshold) {
            val increaseRate = calculateDynamicIncreaseRate(threshold)
            val increase = (threshold * increaseRate).toInt()
            threshold += increase
        }

        return threshold
    }

    fun calculateProgressToNextLevel(points: Int): Double {
        val currentThreshold = getThresholdForPoints(points)
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

        return (progressPoints.toDouble() / levelRange)
    }

    fun crecimientoExponencial(numero: Int): Int {
        return when {
            numero <= 4 -> numero
            numero >= 15 -> 50_000_000
            else -> {
                // Usamos el número de entrada como factor multiplicativo
                val base = 1000.0
                val exponente = (numero - 4) / 2.0
                val factor = numero.toDouble()
                val resultado = (factor * base * Math.pow(base, exponente)).toInt()
                resultado.coerceAtMost(50_000_000)
            }
        }
    }
}