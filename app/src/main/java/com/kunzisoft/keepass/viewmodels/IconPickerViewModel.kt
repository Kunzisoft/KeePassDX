package com.kunzisoft.keepass.viewmodels

import android.os.Parcel
import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.icon.IconImageStandard
import com.kunzisoft.keepass.utils.readParcelableCompat


class IconPickerViewModel: ViewModel() {

    val standardIconPicked: MutableLiveData<IconImageStandard> by lazy {
        MutableLiveData<IconImageStandard>()
    }

    val customIconPicked: MutableLiveData<IconImageCustom> by lazy {
        MutableLiveData<IconImageCustom>()
    }

    val customIconsSelected: MutableLiveData<List<IconImageCustom>> by lazy {
        MutableLiveData<List<IconImageCustom>>()
    }

    val customIconAdded: MutableLiveData<IconCustomState> by lazy {
        MutableLiveData<IconCustomState>()
    }

    val customIconRemoved: MutableLiveData<IconCustomState> by lazy {
        MutableLiveData<IconCustomState>()
    }

    val customIconUpdated : MutableLiveData<IconCustomState> by lazy {
        MutableLiveData<IconCustomState>()
    }

    fun pickStandardIcon(icon: IconImageStandard) {
        standardIconPicked.value = icon
    }

    fun pickCustomIcon(icon: IconImageCustom) {
        customIconPicked.value = icon
    }

    fun selectCustomIcons(icons: List<IconImageCustom>) {
        customIconsSelected.value = icons
    }

    fun deselectAllCustomIcons() {
        customIconsSelected.value = listOf()
    }

    fun addCustomIcon(customIcon: IconCustomState) {
        customIconAdded.value = customIcon
    }

    fun removeCustomIcon(customIcon: IconCustomState) {
        customIconRemoved.value = customIcon
    }

    fun updateCustomIcon(customIcon: IconCustomState) {
        customIconUpdated.value = customIcon
    }

    data class IconCustomState(var iconCustom: IconImageCustom? = null,
                               var error: Boolean = true,
                               var errorStringId: Int = -1,
                               var errorConsumed: Boolean = false): Parcelable {

        constructor(parcel: Parcel) : this(
                parcel.readParcelableCompat(),
                parcel.readByte() != 0.toByte(),
                parcel.readInt(),
                parcel.readByte() != 0.toByte())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeParcelable(iconCustom, flags)
            parcel.writeByte(if (error) 1 else 0)
            parcel.writeInt(errorStringId)
            parcel.writeByte(if (errorConsumed) 1 else 0)
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