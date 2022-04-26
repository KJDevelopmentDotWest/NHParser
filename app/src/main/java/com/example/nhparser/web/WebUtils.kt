package com.example.nhparser.web

import com.example.nhparser.exception.NHException
import com.example.nhparser.model.Manga
import org.apache.commons.lang3.RegExUtils
import org.apache.commons.lang3.StringUtils
import org.jsoup.Connection
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.lang.Exception
import java.net.URL
import java.util.function.Predicate

/**
 * WebUtils class provides methods for interaction with web
 * This class is singleton
 */
class WebUtils private constructor() {

    companion object {
        private val instance: WebUtils = WebUtils()

        /**
         * Returns instance of WebUtils
         */
        @Synchronized
        fun getInstance(): WebUtils {
            return instance
        }
    }

    /**
     * Method sets necessary parameters to manga
     * @param manga target manga. manga.url cannot be null
     */
    fun setMangaParameters(manga: Manga){
        try {
            val document: Document = Jsoup.connect("${manga.url.toString()}/").get()
            var title = document.body().select("span[class=pretty]").first().ownText().toString()
            title = RegExUtils.removeAll(title, "[^(A-Za-z)]")
            if (title.isEmpty()){
                title = "manga"
            }
            manga.title = title

            var containerNum : Int = -1
            do {
                containerNum++
                val containerName = document.body().select("div[id=content]").select("div[id=bigcontainer]").select("div[id=info-block]")
                        .select("div[id=info]").select("section[id=tags]").select("div[class=tag-container field-name]")[containerNum].ownText().toString()
            } while (containerName != "Pages:")
            manga.pagesAmount = document.body().select("div[id=content]").select("div[id=bigcontainer]").select("div[id=info-block]")
                    .select("div[id=info]").select("section[id=tags]").select("div[class=tag-container field-name]")[containerNum]
                    .select("span[class=name]").first().ownText().toInt()

            containerNum = -1
            do {
                containerNum++
                val containerName = document.body().select("div[id=content]").select("div[id=bigcontainer]").select("div[id=info-block]")
                        .select("div[id=info]").select("section[id=tags]").select("div[class=tag-container field-name]")[containerNum].ownText().toString()
            } while (containerName != "Languages:")
            manga.language = document.body().select("div[id=content]").select("div[id=bigcontainer]").select("div[id=info-block]")
                    .select("div[id=info]").select("section[id=tags]").select("div[class=tag-container field-name]")[containerNum]
                    .select("span[class=name]").last().ownText().toString()

            manga.imagesUrl = findImageURLs(manga.url!!, manga.pagesAmount!!)
            manga.cover = getCover(document)

        } catch (e: HttpStatusException) {
            throw NHException("something went wrong when connecting to nhentai. you can google ${e.statusCode} http status code for possible reason")
        } catch (e: NHException) {
            throw NHException(e.message)
        } catch (e: Exception) {
            throw NHException("unexpected exception")
        }
    }

    /**
     * Returns image as byte array
     * @param url image URL
     */
    @Throws(NHException::class)
    fun getImageFromUrl(url: String) : ByteArray {
        var resultImageResponse: Connection.Response?
        try {
            resultImageResponse = Jsoup.connect(url).ignoreContentType(true).execute()
        } catch (e: HttpStatusException) {
            if (e.statusCode == 404) {
                resultImageResponse = when {
                    url.contains("jpg") -> {
                        Jsoup.connect(url.replace("jpg", "png", true)).ignoreContentType(true).execute()
                    }
                    url.contains("png") -> {
                        Jsoup.connect(url.replace("png", "jpg", true)).ignoreContentType(true).execute()
                    }
                    else -> {
                        throw NHException("cannot download image. possible reason that image type cannot be managed by app")
                    }
                }
            } else {
                throw NHException("something went wrong when connecting to nhentai. you can google ${e.statusCode} http status code for possible reason")
            }
        }
        return resultImageResponse!!.bodyAsBytes()
    }

    private fun findImageURLs(mangaURL: URL, pagesAmount: Int): List<String> {
        val imageURLs: ArrayList<String> = arrayListOf()
        val document: Document = Jsoup.connect("$mangaURL/2/").get()
        val pageImageSectionString: String = document.body().select("div[id=content]").first()
            .select("section[id=image-container]").first().toString()
        val firstImageURL =
            StringUtils.substringBetween(pageImageSectionString, "img src=\"", "\"")
        for (i in 1..pagesAmount) {
            val string = StringBuilder()
            string.append(firstImageURL)
            string.deleteCharAt(string.length - 5)
            string.insert(string.length - 4, i)
            imageURLs.add(string.toString())
        }
        return imageURLs
    }

    @Throws(NHException::class)
    private fun getCover(document: Document) : ByteArray {
        val coverSectionString: String = document.body().select("div[id=bigcontainer]").first()
                .select("div[id=cover]").first().toString()
        val predicateIsCover = Predicate<String>{
            it.contains("cover")
        }
        val coverUrl = StringUtils.substringsBetween(coverSectionString, "data-src=\"", "\"")
                .asList().stream().filter(predicateIsCover).findFirst().get()

        try {
            return Jsoup.connect(coverUrl).ignoreContentType(true).execute().bodyAsBytes()
        } catch (e: HttpStatusException){
            throw NHException("cover cannot be found")
        }
    }
}