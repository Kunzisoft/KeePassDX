package com.kunzisoft.keepass.database.element.icon

interface IconImageDraw {
    /**
     * Only to retrieve an icon image to Draw, to not use as object to manipulate
     */
    fun getIconImageToDraw(): IconImage
}