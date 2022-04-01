package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class KeyGeneratorViewModel: ViewModel() {

    val keyGenerated : LiveData<String> get() = _keyGenerated
    private val _keyGenerated = MutableLiveData<String>()

    val keyGeneratedValidated : LiveData<Void?> get() = _keyGeneratedValidated
    private val _keyGeneratedValidated = SingleLiveEvent<Void?>()

    val requireKeyGeneration : LiveData<Void?> get() = _requireKeyGeneration
    private val _requireKeyGeneration = SingleLiveEvent<Void?>()

    fun setKeyGenerated(passKey: String) {
        _keyGenerated.value = passKey
    }

    fun validateKeyGenerated() {
        _keyGeneratedValidated.call()
    }

    fun requireKeyGeneration() {
        _requireKeyGeneration.call()
    }
}