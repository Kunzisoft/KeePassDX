package com.kunzisoft.keepass.activities

import android.os.Bundle
import android.widget.ImageView
import com.igreenwood.loupe.Loupe
import com.kunzisoft.keepass.R
import com.kunzisoft.keepass.activities.lock.LockingActivity
import kotlinx.android.synthetic.main.activity_image_viewer.*

class ImageViewerActivity : LockingActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image_viewer)

        Loupe.create(image_viewer_image, image_viewer_container) {
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
}