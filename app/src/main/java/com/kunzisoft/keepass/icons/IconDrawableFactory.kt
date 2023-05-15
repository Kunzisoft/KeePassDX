/*
 * Copyright 2019 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.icons

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.widget.ImageViewCompat
import com.kunzisoft.keepass.database.element.binary.BinaryCache
import com.kunzisoft.keepass.database.element.binary.BinaryData
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.icon.IconImageDraw
import com.kunzisoft.keepass.icon.IconPack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.*

/**
 * Factory class who build database icons dynamically, can assign an icon of IconPack, or a custom icon to an ImageView with a tint
 */
class IconDrawableFactory(
    private val retrieveBinaryCache: () -> BinaryCache?,
    private val retrieveCustomIconBinary: (iconId: UUID) -> BinaryData?,
) {

    /** customIconMap
     * Cache for icon drawable.
     * Keys: UUID, Values: Drawables
     */
    private val customIconMap = HashMap<UUID, WeakReference<Drawable>>()

    /** standardIconMap
     * Cache for icon drawable.
     * Keys: Integer, Values: Drawables
     */
    private val standardIconMap = HashMap<CacheKey, WeakReference<Drawable>>()

    /**
     * To load an icon pack only if current one is different
     */
    private var mCurrentIconPack: IconPack? = null

    /**
     * Get the [SuperDrawable] [iconDraw] (from cache, or build it and add it to the cache if not exists yet), then tint it with [tintColor] if needed
     */
    private fun getIconSuperDrawable(
        context: Context,
        iconDraw: IconImageDraw,
        width: Int,
        tintColor: Int = Color.WHITE,
    ): SuperDrawable {
        val icon = iconDraw.getIconImageToDraw()
        val customIconBinary = retrieveCustomIconBinary(icon.custom.uuid)
        val binaryCache = retrieveBinaryCache()
        if (binaryCache != null && customIconBinary != null && customIconBinary.dataExists()) {
            getIconDrawable(context.resources, icon.custom, customIconBinary)?.let {
                return SuperDrawable(it)
            }
        }
        val iconPack = IconPackChooser.getSelectedIconPack(context)
        if (mCurrentIconPack != iconPack) {
            this.mCurrentIconPack = iconPack
            this.clearCache()
        }
        iconPack?.iconToResId(icon.standard.id)?.let { iconId ->
            return SuperDrawable(getIconDrawable(context.resources, iconId, width, tintColor),
                iconPack.tintable())
        } ?: run {
            return SuperDrawable(PatternIcon(IconPackChooser.defaultIconSize).blankDrawable)
        }
    }

    /**
     * Build a custom [Drawable] from custom [icon]
     */
    private fun getIconDrawable(
        resources: Resources,
        icon: IconImageCustom,
        iconCustomBinary: BinaryData?,
    ): Drawable? {
        val patternIcon = PatternIcon(IconPackChooser.defaultIconSize)
        retrieveBinaryCache()?.let { binaryCache ->
            val draw: Drawable? = customIconMap[icon.uuid]?.get()
            if (draw == null) {
                iconCustomBinary?.let { binaryFile ->
                    try {
                        var bitmap: Bitmap? =
                            BitmapFactory.decodeStream(binaryFile.getInputDataStream(binaryCache))
                        bitmap?.let { bitmapIcon ->
                            bitmap = resize(bitmapIcon, patternIcon)
                            val createdDraw = BitmapDrawable(resources, bitmap)
                            customIconMap[icon.uuid] = WeakReference(createdDraw)
                            return createdDraw
                        }
                    } catch (e: Exception) {
                        customIconMap.remove(icon.uuid)
                        Log.e(TAG, "Unable to create the bitmap icon", e)
                    }
                }
            } else {
                return draw
            }
        }
        return null
    }

    /**
     * Get the standard [Drawable] icon from [iconId] (cache or build it and add it to the cache if not exists yet)
     * , then tint it with [tintColor] if needed
     */
    private fun getIconDrawable(
        resources: Resources,
        iconId: Int,
        width: Int,
        tintColor: Int,
    ): Drawable {
        val newCacheKey = CacheKey(iconId, width, true, tintColor)

        var draw: Drawable? = standardIconMap[newCacheKey]?.get()
        if (draw == null) {
            try {
                draw = ResourcesCompat.getDrawable(resources, iconId, null)
            } catch (e: Exception) {
                Log.e(TAG, "Can't get icon", e)
            }

            if (draw != null) {
                standardIconMap[newCacheKey] = WeakReference(draw)
            }
        }

        if (draw == null) {
            draw = PatternIcon(IconPackChooser.defaultIconSize).blankDrawable
        }
        draw.isFilterBitmap = false

        return draw
    }

    /**
     * Resize the custom icon to match the built in icons
     *
     * @param bitmap Bitmap to resize
     * @return Bitmap resized
     */
    private fun resize(bitmap: Bitmap, dimensionPattern: PatternIcon): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        return if (width == dimensionPattern.width && height == dimensionPattern.height) {
            bitmap
        } else Bitmap.createScaledBitmap(bitmap,
            dimensionPattern.width,
            dimensionPattern.height,
            true)

    }

    /**
     * Assign a database [icon] to an ImageView and tint it with [tintColor] if needed
     */
    fun assignDatabaseIcon(
        imageView: ImageView,
        icon: IconImageDraw,
        tintColor: Int = Color.WHITE,
    ) {
        try {
            val context = imageView.context
            CoroutineScope(Dispatchers.IO).launch {
                addToCustomCache(context.resources, icon)
                withContext(Dispatchers.Main) {
                    val superDrawable = getIconSuperDrawable(context,
                        icon,
                        imageView.width,
                        tintColor)
                    imageView.setImageDrawable(superDrawable.drawable)
                    if (superDrawable.tintable) {
                        ImageViewCompat.setImageTintList(imageView,
                            ColorStateList.valueOf(tintColor))
                    } else {
                        ImageViewCompat.setImageTintList(imageView, null)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(ImageView::class.java.name, "Unable to assign icon in image view", e)
        }
    }

    /**
     * Build a bitmap from a database [icon]
     */
    fun getBitmapFromIcon(
        context: Context,
        icon: IconImageDraw,
        tintColor: Int = Color.BLACK,
    ): Bitmap? {
        try {
            val superDrawable = getIconSuperDrawable(context,
                icon,
                24,
                tintColor)
            val bitmap = superDrawable.drawable.toBitmap()
            // Tint bitmap if it's not a custom icon
            if (superDrawable.tintable && bitmap.isMutable) {
                Canvas(bitmap).drawBitmap(bitmap, 0.0F, 0.0F, Paint().apply {
                    colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
                })
            }
            return bitmap
        } catch (e: Exception) {
            Log.e(RemoteViews::class.java.name, "Unable to create bitmap from icon", e)
        }
        return null
    }

    /**
     * Simple method to init the cache with the custom icon and be much faster next time
     */
    private fun addToCustomCache(resources: Resources, iconDraw: IconImageDraw) {
        val icon = iconDraw.getIconImageToDraw()
        val customIconBinary = retrieveCustomIconBinary(icon.custom.uuid)
        if (customIconBinary != null
            && customIconBinary.dataExists()
            && !customIconMap.containsKey(icon.custom.uuid)
        )
            getIconDrawable(resources, icon.custom, customIconBinary)
    }

    /**
     * Clear a specific icon from the cache
     */
    fun clearFromCache(icon: IconImageCustom) {
        customIconMap.remove(icon.uuid)
    }

    /**
     * Clear the cache of icons
     */
    fun clearCache() {
        standardIconMap.clear()
        customIconMap.clear()
    }

    /**
     * Build a blankDrawable drawable
     * @param res Resource to build the drawable
     */
    private class PatternIcon(defaultIconSize : Int) {

        var blankDrawable: Drawable = ColorDrawable(Color.TRANSPARENT)
        var width = -1
        var height = -1

        init {
            width = defaultIconSize
            height = defaultIconSize
            blankDrawable.setBounds(0, 0, width, height)
        }
    }

    /**
     * Utility class to prevent a custom icon to be tint
     */
    class SuperDrawable(var drawable: Drawable, var tintable: Boolean = false)

    /**
     * Key class to retrieve a Drawable in the cache if it's tinted or not
     */
    private inner class CacheKey(
        var resId: Int,
        var density: Int,
        var isTint: Boolean,
        var color: Int,
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val cacheKey = other as CacheKey
            return if (isTint)
                resId == cacheKey.resId
                        && density == cacheKey.density
                        && cacheKey.isTint
                        && color == cacheKey.color
            else
                resId == cacheKey.resId
                        && density == cacheKey.density
                        && !cacheKey.isTint
        }

        override fun hashCode(): Int {
            var result = resId
            result = 31 * result + density
            result = 31 * result + isTint.hashCode()
            result = 31 * result + color
            return result
        }
    }

    companion object {

        private val TAG = IconDrawableFactory::class.java.name
    }

}
