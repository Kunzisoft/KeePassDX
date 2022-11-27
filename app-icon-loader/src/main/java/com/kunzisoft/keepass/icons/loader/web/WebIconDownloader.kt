package com.kunzisoft.keepass.icons.loader.web

import android.graphics.BitmapFactory
import com.kunzisoft.keepass.icons.loader.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.*

/**
 * Download web icon from Google.
 */
class WebIconDownloader(
    private val source: IconSource,
    private val serviceUrl: (host: String) -> String,
    private val db: IconDao,
    private val writer: IconWriter,
    private val client: OkHttpClient,
) : IconsDownloader<String>, IconDownloader<String> {

    override fun download(items: Set<String>): List<Icon> {
        val existingIcons = db.getSourceKeys(source)
        return items
            .filterNot { existingIcons.contains(it) }
            .mapNotNull { host -> download(host) }
    }

    override fun download(item: String): Icon? {
        val host = URLEncoder.encode(item, Charsets.UTF_8.name())
        val response = client.newCall(
            request = Request.Builder()
                .url(serviceUrl(host))
                .build()
        ).execute()

        return response.body?.byteStream()?.use { body ->
            BitmapFactory.decodeStream(body)?.let { bitmap ->
                Icon(
                    uuid = UUID.randomUUID(),
                    name = item,
                    sourceKey = item,
                    source = source,
                ).also { icon ->
                    writer.write(icon, bitmap)
                }
            }
        }
    }
}
