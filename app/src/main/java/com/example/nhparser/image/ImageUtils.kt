package com.example.nhparser.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * ImageUtils class provides methods for image processing
 * This class is singleton
 */
class ImageUtils private constructor(){

    companion object {

        private val instance: ImageUtils = ImageUtils()

        /**
         * Returns instance of TextUtils
         */
        @Synchronized
        fun getInstance(): ImageUtils {
            return instance
        }
    }

    /**
     * Returns scaled bitmap
     * @param image image as byte array
     * @param targetWidth width of result bitmap
     * @param targetHeight height of result bitmap
     */
    fun getScaledBitmap(image: ByteArray, targetWidth: Int, targetHeight: Int) : Bitmap {
        val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false)
    }

}