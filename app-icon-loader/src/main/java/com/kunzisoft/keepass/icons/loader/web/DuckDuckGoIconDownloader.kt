package com.kunzisoft.keepass.icons.loader.web

import com.kunzisoft.keepass.icons.loader.IconDao
import com.kunzisoft.keepass.icons.loader.IconSource
import com.kunzisoft.keepass.icons.loader.IconWriter
import com.kunzisoft.keepass.icons.loader.IconsDownloader
import okhttp3.OkHttpClient

/**
 * Download web icon from DuckDuckGo.
 */
class DuckDuckGoIconDownloader(
    private val db: IconDao,
    private val writer: IconWriter,
    private val client: OkHttpClient = OkHttpClient(),
) : IconsDownloader<String> by WebIconDownloader(
    source = IconSource.DuckDuckGo,
    serviceUrl = { host -> "https://icons.duckduckgo.com/ip3/$host.ico" },
    db = db,
    writer = writer,
    client = client,
)
