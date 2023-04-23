package com.kunzisoft.keepass.icons.loader.web

import com.kunzisoft.keepass.icons.loader.IconDao
import com.kunzisoft.keepass.icons.loader.IconSource
import com.kunzisoft.keepass.icons.loader.IconWriter
import com.kunzisoft.keepass.icons.loader.IconsDownloader
import okhttp3.OkHttpClient

/**
 * Download web icon from Google.
 */
class GoogleWebIconDownloader(
    private val db: IconDao,
    private val writer: IconWriter,
    private val client: OkHttpClient = OkHttpClient(),
) : IconsDownloader<String> by WebIconDownloader(
    source = IconSource.Google,
    serviceUrl = { host -> "https://s2.googleusercontent.com/s2/favicons?domain=$host&sz=64" },
    db = db,
    writer = writer,
    client = client,
)
