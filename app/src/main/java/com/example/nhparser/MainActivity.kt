package com.example.nhparser

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.net.URL
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.*
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private lateinit var editTextNumber: EditText
    private lateinit var searchButton : Button
    private lateinit var randomButton : Button
    private lateinit var downloadButton : Button
    private lateinit var mangaURL : URL
    private lateinit var textView: TextView
    private var amountOfPages : Int = 0
    private var urlsArray: ArrayList<String> = arrayListOf()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editTextNumber = findViewById(R.id.editTextNumber)
        searchButton = findViewById(R.id.searchButton)
        randomButton = findViewById(R.id.randomButton)
        downloadButton = findViewById(R.id.downloadButton)
        textView = findViewById(R.id.textView)

        editTextNumber.setText("177013")
        mangaURL = URL("https://nhentai.net/g/177013")
        textView.setTextColor(Color.rgb(200,0,0))


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

            mangaURL = URL("https://nhentai.net/g/$id/")
            Toast.makeText(this, "Searching for $mangaURL", Toast.LENGTH_LONG).show()
            getMangaInfo()
        }
        downloadButton.setOnClickListener{
            findImageURLs()
        }

    }

    private fun getMangaInfo() {
        Thread {
            val stringBuilder = StringBuilder()
            try {
                val doc: Document = Jsoup.connect(mangaURL.toString()).get()
                val title = doc.body().select("span[class=pretty]").first().ownText().toString()
                stringBuilder.append("Title: ").append(title).append("\n")

                var containerNum : Int = -1
                do {
                    containerNum++
                    val containerName = doc.body().select("div[id=content]").select("div[id=bigcontainer]").select("div[id=info-block]").select("div[id=info]").select("section[id=tags]").select("div[class=tag-container field-name]")[containerNum].ownText().toString()
                } while (containerName != "Pages:")
                amountOfPages = doc.body().select("div[id=content]").select("div[id=bigcontainer]").select("div[id=info-block]").select("div[id=info]").select("section[id=tags]").select("div[class=tag-container field-name]")[containerNum].select("span[class=name]").first().ownText().toInt()

                containerNum = -1
                do {
                    containerNum++
                    val containerName = doc.body().select("div[id=content]").select("div[id=bigcontainer]").select("div[id=info-block]").select("div[id=info]").select("section[id=tags]").select("div[class=tag-container field-name]")[containerNum].ownText().toString()
                } while (containerName != "Languages:")
                val language = doc.body().select("div[id=content]").select("div[id=bigcontainer]").select("div[id=info-block]").select("div[id=info]").select("section[id=tags]").select("div[class=tag-container field-name]")[containerNum].select("span[class=name]").last().ownText().toString()

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
        }.start()
    }

    private fun findImageURLs(){
        urlsArray.clear()

        Thread{
            val doc : Document = Jsoup.connect("$mangaURL/1").get()
            val imgURL = doc.body().select("div[id=content]").select("section[id=image-container]").select("img[src]").first().attr("src").toString()
            try {
                for (i in 1 .. amountOfPages){
                    val string = StringBuilder()
                    string.append(imgURL)
                    string.deleteCharAt(string.length-5)
                    string.insert(string.length-4, i)
                    urlsArray.add(string.toString())
                }
            } catch (e: Exception){ }

        }.start()

        downloadImages()
    }

    private fun downloadImages(){

        urlsArray.forEach {
            val input : InputStream = URL(it).openStream()
            val output : OutputStream = BufferedOutputStream(FileOutputStream("/storage/emulated/0/Download"))
            output.write(input.read())
            output.close()
            input.close()
        }


    }
}