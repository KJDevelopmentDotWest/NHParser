package com.example.nhparser.model

import java.net.URL

/**
 * Data class that represents manga
 */
class Manga {
    var url: URL? = null
    var title: String? = null
    var language: String? = null
    var pagesAmount: Int? = null
    var imagesUrl: List<String>? = null
    var cover: ByteArray? = null
}