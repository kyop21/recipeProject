package com.example.recipeproject.util

object KoreanUtil {
    fun getChosung(ch: Char): String {
        val code = ch.code
        if (code !in 0xAC00..0xD7A3) return ch.toString()
        val base = code - 0xAC00
        val index = base / (21 * 28)
        val list = listOf("ㄱ","ㄲ","ㄴ","ㄷ","ㄸ","ㄹ","ㅁ","ㅂ","ㅃ","ㅅ","ㅆ","ㅇ","ㅈ","ㅉ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ")
        return list[index]
    }
}