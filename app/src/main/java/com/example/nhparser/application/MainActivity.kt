package com.example.nhparser.application

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.nhparser.R
import com.example.nhparser.exception.NHException
import com.example.nhparser.image.ImageUtils
import com.example.nhparser.io.FileIO
import com.example.nhparser.model.Manga
import com.example.nhparser.text.TextUtils
import com.example.nhparser.web.WebUtils
import java.net.URL
import java.util.Random
import java.util.concurrent.Executors


open class MainActivity : AppCompatActivity() {

    private lateinit var editText: EditText
    private lateinit var searchButton : Button
    private lateinit var randomButton : Button
    private lateinit var downloadButton : Button
    private lateinit var infoTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var coverImageView: ImageView

    private val executorService = Executors.newFixedThreadPool(4)

    override fun onCreate(savedInstanceState: Bundle?) {
        val manga = Manga()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editText = findViewById(R.id.editText)
        searchButton = findViewById(R.id.searchButton)
        randomButton = findViewById(R.id.randomButton)
        downloadButton = findViewById(R.id.downloadButton)
        infoTextView = findViewById(R.id.infoTextView)
        statusTextView = findViewById(R.id.statusTextView)
        coverImageView = findViewById(R.id.coverImageView)

        editText.setText("https://nhentai.net/g/309246")
        manga.url = URL("https://nhentai.net/g/309246")

        checkPermissions()

        searchButton.setOnClickListener {
            try {
                val url = TextUtils.getInstance().resolveInputAndReturnUrl(editText.text.toString())
                manga.url = URL(url)
                editText.setText(url, TextView.BufferType.EDITABLE)
                Toast.makeText(this, "Searching for ${manga.url}", Toast.LENGTH_SHORT).show()
                executorService.submit(SearchButtonClickRunnable(manga, this))
            } catch (e: NHException){
                infoTextView.text = e.message
                statusTextView.text = ""
                coverImageView.setImageResource(0)
            }
        }

        randomButton.setOnClickListener {
            val random = Random()
            val id = random.nextInt(239999) + 100000
            editText.setText("$id")
            searchButton.performClick()
        }

        downloadButton.setOnClickListener{
            if (manga.imagesUrl == null || manga.title == null) {
                searchButton.performClick()
            }
            Toast.makeText(this, "Download started. This will take a while, you can minimize application", Toast.LENGTH_LONG).show()
            executorService.submit(DownloadButtonClickRunnable(manga, this))
        }
    }

    private fun checkPermissions() {
        @RequiresApi(Build.VERSION_CODES.R)
        if (!Environment.isExternalStorageManager()){
            Toast.makeText(this, "app requires manage all files permission for android 11+ devices due to google security policy changes", Toast.LENGTH_LONG).show()
            val permissionIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(permissionIntent)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET), 1)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
    }

    private class DownloadButtonClickRunnable(private var manga: Manga,
                                              private var activity: MainActivity) : Runnable{
        private val fileIOProvider = FileIO.getInstance()

        override fun run() {
            try {
                activity.runOnUiThread {
                    activity.statusTextView.text = "Downloading"
                    activity.downloadButton.isEnabled = false
                    activity.searchButton.isEnabled = false
                    activity.randomButton.isEnabled = false
                }
                fileIOProvider.downloadImages(manga.imagesUrl!!, manga.title!!)
                activity.runOnUiThread {
                    activity.statusTextView.text = "Success"
                }
            } catch (e: NHException){
                activity.runOnUiThread {
                    activity.infoTextView.text = e.message
                }
            } finally {
                activity.runOnUiThread {
                    activity.downloadButton.isEnabled = true
                    activity.searchButton.isEnabled = true
                    activity.randomButton.isEnabled = true
                }
            }
        }
    }

    private class SearchButtonClickRunnable(private var manga: Manga,
                                            private var activity: MainActivity): Runnable {
        private val webUtils = WebUtils.getInstance()

        override fun run() {
            try {
                activity.runOnUiThread {
                    activity.downloadButton.isEnabled = false
                    activity.statusTextView.text = "Searching"
                }
                webUtils.setMangaParameters(manga)
                activity.runOnUiThread {
                    printMangaInfo(manga)
                    activity.statusTextView.text = "Found"

                    val coverWidth = Resources.getSystem().displayMetrics.widthPixels.times(0.7)
                    val coverHeight = coverWidth.times(1.77)
                    activity.coverImageView.setImageBitmap(ImageUtils.getInstance().getScaledBitmap(manga.cover!!, coverWidth.toInt(), coverHeight.toInt()))
                }
            } catch (e: NHException){
                activity.runOnUiThread {
                    activity.infoTextView.text = e.message
                }
            } finally {
                activity.runOnUiThread {
                    activity.downloadButton.isEnabled = true
                }
            }
        }

        private fun printMangaInfo(manga: Manga){
            val contentStringBuilder = StringBuilder()
            contentStringBuilder.append("Download Location : \n/Download/${manga.title}").append("\n")
            contentStringBuilder.append("Title: ").append(manga.title).append("\n")
            contentStringBuilder.append("Pages: ").append(manga.pagesAmount).append("\n")
            contentStringBuilder.append("Language: ").append(manga.language).append("\n")
            activity.infoTextView.text = contentStringBuilder.toString()
        }
    }

    override fun onDestroy(){
        executorService.shutdownNow()
        super.onDestroy()
    }
}
