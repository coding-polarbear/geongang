package com.example.geongang

object Converter {
    fun convertTime(time: Float): String {
        if(time > 60) {
            var minute = time / 60
            var second = time % 60
            return "${minute}분 #{second}초"
        } else {
            return "${time}초"
        }
    }
}