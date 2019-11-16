package com.example.geongang.utils

import kotlin.math.roundToInt

object Converter {
    fun convertTime(time: Float): String {
        if(time > 60) {
            var minute = time / 60
            var second = time % 60
            return "${minute.toInt()}분 #{second}초"
        } else {
            return "${time.toInt()}초"
        }
    }
}