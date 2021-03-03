package com.kunzisoft.keepass.viewmodels

import android.content.ContentResolver
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.tasks.BinaryStreamManager.resizeBitmapAndStoreDataInBinaryFile
import kotlinx.coroutines.*
import java.io.File


class IconPickerViewModel: ViewModel() {

    private val mainScope = CoroutineScope(Dispatchers.Main)

    val iconStandardSelected: MutableLiveData<IconImageStandard> by lazy {
        MutableLiveData<IconImageStandard>()
    }

    val iconCustomSelected: MutableLiveData<IconImageCustom> by lazy {
        MutableLiveData<IconImageCustom>()
    }

    val iconCustomAdded: MutableLiveData<IconCustomState> by lazy {
        MutableLiveData<IconCustomState>()
    }

    fun selectIconStandard(icon: IconImageStandard) {
        iconStandardSelected.value = icon
    }

    fun selectIconCustom(icon: IconImageCustom) {
        iconCustomSelected.value = icon
    }

    fun addCustomIcon(database: Database,
                      contentResolver: ContentResolver,
                      iconDir: File,
                      iconToUploadUri: Uri) {
        mainScope.launch {
            withContext(Dispatchers.IO) {
                // on Progress with thread
                val asyncResult: Deferred<IconImageCustom?> = async {
                    database.buildNewCustomIcon(iconDir)?.let { customIcon ->
                        resizeBitmapAndStoreDataInBinaryFile(contentResolver,
                                iconToUploadUri, customIcon.binaryFile)
                        customIcon
                    }
                }
                withContext(Dispatchers.Main) {
                    asyncResult.await()?.let { customIcon ->
                        var error = false
                        if (customIcon.binaryFile.length <= 0) {
                            database.removeCustomIcon(customIcon.uuid)
                            error = true
                        }
                        iconCustomAdded.value = IconCustomState(customIcon, error)
                    }
                }
            }
        }
    }

    data class IconCustomState(val iconCustom: IconImageCustom, val error: Boolean): Parcelable {

        constructor(parcel: Parcel) : this(
                parcel.readParcelable(IconImageCustom::class.java.classLoader) ?: IconImageCustom(),
                parcel.readByte() != 0.toByte())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeParcelable(iconCustom, flags)
            parcel.writeByte(if (error) 1 else 0)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<IconCustomState> {
            override fun createFromParcel(parcel: Parcel): IconCustomState {
                return IconCustomState(parcel)
            }

            override fun newArray(size: Int): Array<IconCustomState?> {
                return arrayOfNulls(size)
            }
        }
    }
}