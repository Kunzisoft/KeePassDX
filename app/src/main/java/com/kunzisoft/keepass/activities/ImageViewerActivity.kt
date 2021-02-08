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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import com.igreenwood.loupe.Loupe
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.lock.LockingActivity
import com.kunzisoft.keepass.database.element.Attachment
import com.kunzisoft.keepass.database.element.Database
import kotlinx.android.synthetic.main.activity_image_viewer.*

class ImageViewerActivity : LockingActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image_viewer)
        val imageView: ImageView = findViewById(R.id.image_viewer_image)
        val progressView: View = findViewById(R.id.image_viewer_progress)

        try {
            progressView.visibility = View.VISIBLE
            intent.getParcelableExtra<Attachment>(IMAGE_ATTACHMENT_TAG)?.let { attachment ->
                Attachment.loadBitmap(attachment, Database.getInstance().loadedCipherKey) { bitmapLoaded ->
                    if (bitmapLoaded == null) {
                        finish()
                    } else {
                        progressView.visibility = View.GONE
                        imageView.setImageBitmap(bitmapLoaded)
                    }
                }
            } ?: finish()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to view the binary", e)
            finish()
        }

        Loupe.create(imageView, image_viewer_container) {
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