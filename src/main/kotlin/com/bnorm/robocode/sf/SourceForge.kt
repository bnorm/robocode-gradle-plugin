package com.bnorm.robocode.sf

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSource

internal object SourceForge {
    private val client = OkHttpClient()
    private val mapper = XmlMapper().apply {
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        registerModule(KotlinModule())
    }

    private const val SOURCEFORGE_ROBOCODE_RSS = "https://sourceforge.net/projects/robocode/rss?limit=2"
    private val ROBOCODE_FILE_REGEX = "/robocode/([0-9.]+)/robocode-([0-9.]+)-setup.jar".toRegex()

    private fun downloadLink(version: String) =
        "https://sourceforge.net/projects/robocode/files/robocode/$version/robocode-$version-setup.jar/download"

    fun download(version: String): BufferedSource {
        val request = Request.Builder().get().url(downloadLink(version)).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) error("")
        return response.body!!.source()
    }

    fun findLatestVersion(): String {
        val request = Request.Builder().get().url(SOURCEFORGE_ROBOCODE_RSS).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) error("")
        val feed = mapper.readValue(response.body!!.string(), RssFeed::class.java)
        val channel = feed.channels.firstOrNull { it.title == "Robocode" } ?: error("")
        val item = channel.items
            .mapNotNull { ROBOCODE_FILE_REGEX.matchEntire(it.title) }
            .firstOrNull() ?: error("")
        return item.destructured.component1()
    }
}
