package com.example.nhparser.text

import com.example.nhparser.exception.NHException
import java.util.regex.Pattern

/**
 * WebUtils class provides methods for text processing
 * This class is singleton
 */
class TextUtils private constructor(){
    companion object {
        private val mangaMainPageURLPattern = Pattern.compile("https://nhentai.net/g/[0-9][0-9][0-9][0-9][0-9][0-9]/?")
        private val manga6DigitCodePattern = Pattern.compile("[0-9][0-9][0-9][0-9][0-9][0-9]")

        private val instance: TextUtils = TextUtils()

        /**
         * Returns instance of TextUtils
         */
        @Synchronized
        fun getInstance(): TextUtils {
            return instance
        }
    }

    /**
     * Returns manga url
     * @param input input string
     * @throws NHException if string is invalid
     */
    @Throws(NHException::class)
    fun resolveInputAndReturnUrl(input: String): String{
        return when {
            mangaMainPageURLPattern.matcher(input).matches() -> {
                input
            }
            manga6DigitCodePattern.matcher(input).matches() -> {
                "https://nhentai.net/g/$input"
            }
            else -> {
                throw NHException("url input incorrect. use full link to manga main page (https://nhentai.net/g/309246) or 6-digit code")
            }
        }
    }
}