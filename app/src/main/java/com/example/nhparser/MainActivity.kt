package com.example.nhparser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var editTextNumber: EditText
    private lateinit var searchButton : Button
    private lateinit var randomButton : Button
    private lateinit var downloadButton : Button
    private lateinit var mangaURL : URL
    private lateinit var textView: TextView
    private lateinit var downLocInfo: TextView
    private lateinit var title: String
    private var amountOfPages : Int = 0
    private var urlsArray: ArrayList<String> = arrayListOf()
    private var bitmapArray : ArrayList<Bitmap?> = arrayListOf()
    private var message: StringBuilder = StringBuilder()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editTextNumber = findViewById(R.id.editTextNumber)
        searchButton = findViewById(R.id.searchButton)
        randomButton = findViewById(R.id.randomButton)
        downloadButton = findViewById(R.id.downloadButton)
        textView = findViewById(R.id.textView)
        downLocInfo = findViewById(R.id.downLocInfo)

        editTextNumber.setText("344040")
        mangaURL = URL("https://nhentai.net/g/344040")

        searchButton.setOnClickListener {
            if (editTextNumber.text.length.compareTo(6) == 0){
                mangaURL = URL("https://nhentai.net/g/" + editTextNumber.text.toString())
                Toast.makeText(this, "Searching for $mangaURL", Toast.LENGTH_SHORT).show()
                getMangaInfo()

            } else {
                Toast.makeText(this, "I said six fucking digits", Toast.LENGTH_SHORT).show()
            }
        }
        randomButton.setOnClickListener {
            val random = Random()
            val id = random.nextInt(239999) + 100000
            editTextNumber.setText("$id")

            mangaURL = URL("https://nhentai.net/g/$id/")
            Toast.makeText(this, "Searching for $mangaURL", Toast.LENGTH_LONG).show()
            getMangaInfo()
        }
        downloadButton.setOnClickListener{
            bitmapArray.clear()
            downloadImages()
        }

    }

    private fun getMangaInfo() {
        bitmapArray.clear()

        val thread = Thread() {
            val stringBuilder = StringBuilder()
            try {
                val doc: Document = Jsoup.connect(mangaURL.toString()).get()
                title = doc.body().select("span[class=pretty]").first().ownText().toString()
                stringBuilder.append("Title: ").append(title).append("\n")

                var containerNum : Int = -1
                do {
                    containerNum++
                    val containerName = doc.body().select("div[id=content]").select("div[id=bigcontainer]").select(
                        "div[id=info-block]"
                    ).select("div[id=info]").select("section[id=tags]").select("div[class=tag-container field-name]")[containerNum].ownText().toString()
                } while (containerName != "Pages:")
                amountOfPages = doc.body().select("div[id=content]").select("div[id=bigcontainer]").select(
                    "div[id=info-block]"
                ).select("div[id=info]").select("section[id=tags]").select("div[class=tag-container field-name]")[containerNum].select(
                    "span[class=name]"
                ).first().ownText().toInt()

                containerNum = -1
                do {
                    containerNum++
                    val containerName = doc.body().select("div[id=content]").select("div[id=bigcontainer]").select(
                        "div[id=info-block]"
                    ).select("div[id=info]").select("section[id=tags]").select("div[class=tag-container field-name]")[containerNum].ownText().toString()
                } while (containerName != "Languages:")
                val language = doc.body().select("div[id=content]").select("div[id=bigcontainer]").select(
                    "div[id=info-block]"
                ).select("div[id=info]").select("section[id=tags]").select("div[class=tag-container field-name]")[containerNum].select(
                    "span[class=name]"
                ).last().ownText().toString()

                stringBuilder.append("Pages Value: ").append(amountOfPages).append("\n")
                stringBuilder.append("Language: ").append(language).append("\n")

            } catch (e: IOException) {
                stringBuilder.append("such manga doesn't exists")
            } catch (e: IndexOutOfBoundsException){
                stringBuilder.append("such manga doesn't exists")
            }
            runOnUiThread {
                textView.text = stringBuilder.toString()
            }
        }

        thread.name = "GetMangaInfoThread"
        thread.start()

        while (thread.isAlive){}

        findImageURLs()
    }

    private fun findImageURLs(){
        urlsArray.clear()

        val thread = Thread(){
            val doc : Document = Jsoup.connect("$mangaURL/1").get()
            val imgURL = doc.body().select("div[id=content]").select("section[id=image-container]").select(
                "img[src]"
            ).first().attr("src").toString()
            try {
                for (i in 1 .. amountOfPages){
                    val string = StringBuilder()
                    string.append(imgURL)
                    string.deleteCharAt(string.length - 5)
                    string.insert(string.length - 4, i)
                    urlsArray.add(string.toString())
                }
            } catch (e: Exception){ }

        }
        thread.name = "FindImageURLsThread"
        thread.start()
    }

    private fun getBitmapFromURL(src: String?): Bitmap? {
        return try {
            val url = URL(src)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            null
        }
    }

    private fun downloadImages(){

        val thread = Thread(){

            runOnUiThread(){
                val downLocInfoText = StringBuilder("${downLocInfo.text} \n/Download/$title")
                downLocInfo.text = downLocInfoText.toString()
                message.append("downloading.... please be patient nhentai has autism")
                textView.text = message.toString()
            }


            urlsArray.forEachIndexed(){ i, it ->

                bitmapArray.add(getBitmapFromURL(it))

                runOnUiThread(){
                    message.clear().append("downloading.... please be patient \nnhentai has autism").append("\n${i+1} of $amountOfPages downloaded")
                    textView.text = message.toString()
                }

            }

            val path = StringBuilder("/storage/emulated/0/Download/$title")
            val directory = File(path.toString())

            if (!directory.exists()) directory.mkdir()

            bitmapArray.forEachIndexed() { i, it ->
                val filename = StringBuilder(path).append("/${i+1}.png")
                val file = File(filename.toString())
                if (!file.exists()) file.createNewFile()

                try {
                    FileOutputStream(filename.toString()).use { out ->
                        it?.compress(
                            Bitmap.CompressFormat.PNG,
                            100,
                            out
                        )
                    }
                } catch (e: IOException) {
                    textView.text = e.toString()
                }

                runOnUiThread(){
                    message.clear().append("downloading.... please be patient nhentai has autism").append("\n${i+1} of $amountOfPages saved to storage")
                    textView.text = message.toString()
                }

            }

            runOnUiThread(){
                message.append("\nSuccess")
                textView.text = message.toString()
            }

        }

        thread.start()
    }
}
