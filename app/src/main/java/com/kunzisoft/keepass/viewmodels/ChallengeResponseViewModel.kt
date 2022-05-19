package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChallengeResponseViewModel: ViewModel() {

    val dataResponded : LiveData<ByteArray?> get() = _dataResponded
    private val _dataResponded = MutableLiveData<ByteArray?>()

    fun respond(byteArray: ByteArray) {
        _dataResponded.value = byteArray
    }

    fun resendResponse() {
        dataResponded.value?.let {
            _dataResponded.value = it
        }
    }

    fun consumeResponse() {
        _dataResponded.value = null
    }
}