package com.example.nhparser.io

import androidx.annotation.NonNull
import com.example.nhparser.exception.NHException
import com.example.nhparser.web.WebUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.math.log10

/**
 * FileIO class provides methods for downloading and saving images
 * This class is singleton
 */
class FileIO private constructor() {

    companion object {
        private val instance: FileIO = FileIO()

        /**
         * Returns instance of FileIO
         */
        @Synchronized
        fun getInstance(): FileIO{
            return instance
        }
    }

    /**
     * Method downloads and saves images to /Download/title directory
     * @param imageUrlsArray urls of images that will be saved
     * @param title title of manga
     * @throws NHException if downloading or saving went wrong
     */
    @Throws(NHException::class)
    fun downloadImages(@NonNull imageUrlsArray: List<String>, @NonNull title: String) {
        val numberOfDigitsInPageNumber = (log10(imageUrlsArray.size.toDouble()) + 1).toInt()

        if (imageUrlsArray.isEmpty()){
            throw NHException("cannot download image. possible reason that image url wasn't found")
        }

        val path = StringBuilder("/storage/emulated/0/Download/${title}")
        val directory = File(path.toString())

        if (!directory.exists()) {
            directory.mkdir()
        }

        imageUrlsArray.forEachIndexed { i, it ->
            val filename = StringBuilder(path).append("/${fillPageNumberWithZerosAtFront(i+1, numberOfDigitsInPageNumber)}.png")
            val file = File(filename.toString())

            if (!file.exists()) {
                file.createNewFile()
            } else {
                file.delete()
            }

            val image = WebUtils.getInstance().getImageFromUrl(it)

            try {
                val outputStream = FileOutputStream(file)

                if (Objects.nonNull(image)){
                    outputStream.write(image)
                } else {
                    throw NHException("cannot download image. possible reason that image url wasn't found")
                }

                outputStream.close()
            } catch (e: IOException) {
                throw NHException("cannot download image. possible reason that image cannot be saved to storage")
            }
        }
    }

    private fun fillPageNumberWithZerosAtFront(page: Int, targetLength: Int): String{
        val result = StringBuilder()
        result.append(page)
        while (result.length < targetLength){
            result.insert(0, "0")
        }
        return result.toString()
    }
}