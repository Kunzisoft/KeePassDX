package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.database.element.icon.IconImageCustom
import com.kunzisoft.keepass.database.element.icon.IconImageStandard

class IconPickerViewModel: ViewModel() {

    val iconStandardSelected: MutableLiveData<IconImageStandard> by lazy {
        MutableLiveData<IconImageStandard>()
    }

    val iconCustomSelected: MutableLiveData<IconImageCustom> by lazy {
        MutableLiveData<IconImageCustom>()
    }

    val iconCustomAdded: MutableLiveData<IconImageCustom> by lazy {
        MutableLiveData<IconImageCustom>()
    }

    fun selectIconStandard(icon: IconImageStandard) {
        iconStandardSelected.value = icon
    }

    fun selectIconCustom(icon: IconImageCustom) {
        iconCustomSelected.value = icon
    }

    fun addCustomIcon(icon: IconImageCustom) {
        iconCustomAdded.value = icon
    }
}