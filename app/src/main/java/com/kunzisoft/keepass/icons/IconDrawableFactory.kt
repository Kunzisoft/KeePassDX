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
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.database.element.icon.IconImage
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import org.apache.commons.collections.map.AbstractReferenceMap
import org.apache.commons.collections.map.ReferenceMap

/**
 * Factory class who build database icons dynamically, can assign an icon of IconPack, or a custom icon to an ImageView with a tint
 */
class IconDrawableFactory {

    /** customIconMap
     * Cache for icon drawable.
     * Keys: UUID, Values: Drawables
     */
    private val customIconMap = ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK)

    /** standardIconMap
     * Cache for icon drawable.
     * Keys: Integer, Values: Drawables
     */
    private val standardIconMap = ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK)

    /**
     * Utility method to assign a drawable to an ImageView and tint it
     */
    fun assignDrawableToImageView(superDrawable: SuperDrawable, imageView: ImageView?, tint: Boolean, tintColor: Int) {
        if (imageView != null) {
            imageView.setImageDrawable(superDrawable.drawable)
            if (superDrawable.tintable && tint) {
                ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(tintColor))
            } else {
                ImageViewCompat.setImageTintList(imageView, null)
            }
        }
    }

    /**
     * Utility method to assign a drawable to a RemoteView and tint it
     */
    fun assignDrawableToRemoteViews(superDrawable: SuperDrawable,
                                    remoteViews: RemoteViews,
                                    imageId: Int,
                                    tintColor: Int = Color.BLACK) {
        val bitmap = superDrawable.drawable.toBitmap()
        // Tint bitmap if it's not a custom icon
        if (superDrawable.tintable && bitmap.isMutable) {
            Canvas(bitmap).drawBitmap(bitmap, 0.0F, 0.0F, Paint().apply {
                colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
            })
        }
        remoteViews.setImageViewBitmap(imageId, bitmap)
    }

    /**
     * Get the [SuperDrawable] [icon] (from cache, or build it and add it to the cache if not exists yet), then [tint] it with [tintColor] if needed
     */
    fun getIconSuperDrawable(context: Context, icon: IconImage, width: Int, tint: Boolean = false, tintColor: Int = Color.WHITE): SuperDrawable {
        return when (icon) {
            is IconImageStandard -> {
                val resId = IconPackChooser.getSelectedIconPack(context)?.iconToResId(icon.iconId) ?: R.drawable.ic_blank_32dp
                getIconSuperDrawable(context, resId, width, tint, tintColor)
            }
            is IconImageCustom -> {
                SuperDrawable(getIconDrawable(context.resources, icon))
            }
            else -> {
                SuperDrawable(PatternIcon(context.resources).blankDrawable)
            }
        }
    }

    /**
     * Get the [SuperDrawable] IconImageStandard from [iconId] (cache, or build it and add it to the cache if not exists yet)
     * , then [tint] it with [tintColor] if needed
     */
    fun getIconSuperDrawable(context: Context, iconId: Int, width: Int, tint: Boolean, tintColor: Int): SuperDrawable {
        return SuperDrawable(getIconDrawable(context.resources, iconId, width, tint, tintColor), true)
    }

    /**
     * Key class to retrieve a Drawable in the cache if it's tinted or not
     */
    private inner class CacheKey(var resId: Int, var density: Int, var isTint: Boolean, var color: Int) {

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

    /**
     * Build a custom [Drawable] from custom [icon]
     */
    private fun getIconDrawable(resources: Resources, icon: IconImageCustom): Drawable {
        val patternIcon = PatternIcon(resources)

        var draw: Drawable? = customIconMap[icon.uuid] as Drawable?
        if (draw == null) {
            var bitmap: Bitmap? = BitmapFactory.decodeByteArray(icon.imageData, 0, icon.imageData.size)
            // Could not understand custom icon
            bitmap?.let { bitmapIcon ->
                bitmap = resize(bitmapIcon, patternIcon)
                draw = BitmapDrawable(resources, bitmap)
                customIconMap[icon.uuid] = draw
                return draw!!
            }
        } else {
            return draw!!
        }
        return patternIcon.blankDrawable
    }

    /**
     * Get the standard [Drawable] icon from [iconId] (cache or build it and add it to the cache if not exists yet)
     * , then [tint] it with [tintColor] if needed
     */
    private fun getIconDrawable(resources: Resources, iconId: Int, width: Int, tint: Boolean, tintColor: Int): Drawable {
        val newCacheKey = CacheKey(iconId, width, tint, tintColor)

        var draw: Drawable? = standardIconMap[newCacheKey] as Drawable?
        if (draw == null) {
            try {
                draw = ResourcesCompat.getDrawable(resources, iconId, null)
            } catch (e: Exception) {
                Log.e(TAG, "Can't get icon", e)
            }

            if (draw != null) {
                standardIconMap[newCacheKey] = draw
            }
        }

        if (draw == null) {
            draw = PatternIcon(resources).blankDrawable
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
        } else Bitmap.createScaledBitmap(bitmap, dimensionPattern.width, dimensionPattern.height, true)

    }

    /**
     * Clear the cache of icons
     */
    fun clearCache() {
        standardIconMap.clear()
        customIconMap.clear()
    }

    private class PatternIcon
    /**
     * Build a blankDrawable drawable
     * @param res Resource to build the drawable
     */(res: Resources) {

        var blankDrawable: Drawable = ColorDrawable(Color.TRANSPARENT)
        var width = -1
        var height = -1

        init {
            width = res.getDimension(R.dimen.icon_size).toInt()
            height = res.getDimension(R.dimen.icon_size).toInt()
            blankDrawable.setBounds(0, 0, width, height)
        }
    }

    /**
     * Utility class to prevent a custom icon to be tint
     */
    class SuperDrawable(var drawable: Drawable, var tintable: Boolean = false)

    companion object {

        private val TAG = IconDrawableFactory::class.java.name
    }

}

/**
 * Assign a default database icon to an ImageView and tint it with [tintColor] if needed
 */
fun ImageView.assignDefaultDatabaseIcon(iconFactory: IconDrawableFactory,
                                        tintColor: Int = Color.WHITE) {
    try {
        IconPackChooser.getSelectedIconPack(context)?.let { selectedIconPack ->
            iconFactory.assignDrawableToImageView(
                    iconFactory.getIconSuperDrawable(context,
                                    selectedIconPack.defaultIconId,
                                    width,
                                    selectedIconPack.tintable(),
                                    tintColor),
                        this,
                        selectedIconPack.tintable(),
                        tintColor)
        }
    } catch (e: Exception) {
        Log.e(ImageView::class.java.name, "Unable to assign icon in image view", e)
    }
}

/**
 * Assign a database [icon] to an ImageView and tint it with [tintColor] if needed
 */
fun ImageView.assignDatabaseIcon(iconFactory: IconDrawableFactory,
                                 icon: IconImage,
                                 tintColor: Int = Color.WHITE) {
    try {
        IconPackChooser.getSelectedIconPack(context)?.let { selectedIconPack ->
            iconFactory.assignDrawableToImageView(
                    iconFactory.getIconSuperDrawable(context,
                            icon,
                            width,
                            true,
                            tintColor),
                        this,
                        selectedIconPack.tintable(),
                        tintColor)
        }
    } catch (e: Exception) {
        Log.e(ImageView::class.java.name, "Unable to assign icon in image view", e)
    }
}

fun RemoteViews.assignDatabaseIcon(context: Context,
                                   imageId: Int,
                                   iconFactory: IconDrawableFactory,
                                   icon: IconImage,
                                   tintColor: Int = Color.BLACK) {
    try {
        iconFactory.assignDrawableToRemoteViews(
                iconFactory.getIconSuperDrawable(context,
                        icon,
                        24,
                        true,
                        tintColor),
                this,
                imageId,
                tintColor)
    } catch (e: Exception) {
        Log.e(RemoteViews::class.java.name, "Unable to assign icon in remote view", e)
    }
}
