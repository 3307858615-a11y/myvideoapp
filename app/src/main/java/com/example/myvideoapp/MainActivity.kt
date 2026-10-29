package com.example.myvideoapp

import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL

class MainActivity : AppCompatActivity() {
    private val videoList = mutableListOf<Pair<String, String>>()
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkSelfPermission(android.Manifest.permission.INTERNET) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.INTERNET), 1)
        }
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val videoView = VideoView(this).apply { 
            layoutParams = LinearLayout.LayoutParams(-1, 600) 
        }
        listView = ListView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }
        layout.addView(videoView)
        layout.addView(listView)
        setContentView(layout)

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            playVideo(videoList[position].second, videoView)
        }

        fetchList(adapter)
    }

    private fun fetchList(adapter: ArrayAdapter<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://bfzyapi.com/api.php/provide/vod/?ac=list")
                val json = JSONObject(url.readText())
                val listArray = json.getJSONArray("list")
                for (i in 0 until listArray.length()) {
                    val item = listArray.getJSONObject(i)
                    videoList.add(Pair(item.getString("vod_name"), item.getString("vod_id")))
                }
                withContext(Dispatchers.Main) {
                    adapter.addAll(videoList.map { it.first })
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun playVideo(id: String, videoView: VideoView) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://bfzyapi.com/api.php/provide/vod/?ac=detail&ids=$id")
                val json = JSONObject(url.readText())
                val playUrlStr = json.getJSONArray("list").getJSONObject(0).getString("vod_play_url")
                val m3u8Url = playUrlStr.split("#")[0].split("$")[1]
                withContext(Dispatchers.Main) {
                    val controller = MediaController(this@MainActivity)
                    controller.setAnchorView(videoView)
                    videoView.setMediaController(controller)
                    videoView.setVideoURI(Uri.parse(m3u8Url))
                    videoView.start()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}
