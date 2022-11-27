package com.kunzisoft.keepass.icons.loader

interface IconDownloader<T> {
    fun download(item: T): Icon?
}

interface IconsDownloader<T> {
    fun download(items: Set<T>): List<Icon>
}
