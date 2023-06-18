/*
 * Copyright 2021 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 * KeePassDX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * KeePassDX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import com.igreenwood.loupe.Loupe
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.legacy.DatabaseLockActivity
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.tasks.BinaryDatabaseManager
import com.kunzisoft.keepass.utils.getParcelableExtraCompat
import kotlin.math.max

class ImageViewerActivity : DatabaseLockActivity() {

    private var imageContainerView: ViewGroup? = null
    private lateinit var imageView: ImageView
    private lateinit var progressView: View

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image_viewer)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setOnTouchListener { _, _ ->
            resetAppTimeout()
            false
        }

        imageContainerView = findViewById(R.id.image_viewer_container)
        imageView = findViewById(R.id.image_viewer_image)
        progressView = findViewById(R.id.image_viewer_progress)

        Loupe.create(imageView, imageContainerView!!) {
            onViewTouchedListener = View.OnTouchListener { _, _ ->
                // to reset timeout when Loupe image view touched
                resetAppTimeout()
                false
            }
            onViewTranslateListener = object : Loupe.OnViewTranslateListener {

                override fun onStart(view: ImageView) {
                    // called when the view starts moving
                }

                override fun onViewTranslate(view: ImageView, amount: Float) {
                    // called whenever the view position changed
                }

                override fun onRestore(view: ImageView) {
                    // called when the view drag gesture ended
                }

                override fun onDismiss(view: ImageView) {
                    // called when the view drag gesture ended
                    finish()
                }
            }
        }
    }

    override fun viewToInvalidateTimeout(): View? {
        // Null to manually manage events
        return null
    }

    override fun finishActivityIfReloadRequested(): Boolean {
        return true
    }

    override fun onDatabaseRetrieved(database: ContextualDatabase?) {
        super.onDatabaseRetrieved(database)

        try {
            progressView.visibility = View.VISIBLE
            intent.getParcelableExtraCompat<Attachment>(IMAGE_ATTACHMENT_TAG)?.let { attachment ->

                supportActionBar?.title = attachment.name

                val size = attachment.binaryData.getSize()
                supportActionBar?.subtitle = Formatter.formatFileSize(this, size)

                // Approximately, to not OOM and allow a zoom
                val mImagePreviewMaxWidth = max(
                    resources.displayMetrics.widthPixels * 2,
                    resources.displayMetrics.heightPixels * 2
                )

                database?.let { database ->
                    BinaryDatabaseManager.loadBitmap(
                        database,
                        attachment.binaryData,
                        mImagePreviewMaxWidth
                    ) { bitmapLoaded ->
                        if (bitmapLoaded == null) {
                            finish()
                        } else {
                            progressView.visibility = View.GONE
                            imageView.setImageBitmap(bitmapLoaded)
                        }
                    }
                }
            } ?: finish()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to view the binary", e)
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        private val TAG = ImageViewerActivity::class.simpleName

        private const val IMAGE_ATTACHMENT_TAG = "IMAGE_ATTACHMENT_TAG"

        fun getInstance(context: Context, imageAttachment: Attachment) {
            context.startActivity(Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(IMAGE_ATTACHMENT_TAG, imageAttachment)
            })
        }
    }
}