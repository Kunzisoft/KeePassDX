/*
 * Copyright 2022 Jeremy Jamet / Kunzisoft.
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

    val passwordGeneratedValidated : LiveData<Void?> get() = _passwordGeneratedValidated
    private val _passwordGeneratedValidated = SingleLiveEvent<Void?>()
    val requirePasswordGeneration : LiveData<Void?> get() = _requirePasswordGeneration
    private val _requirePasswordGeneration = SingleLiveEvent<Void?>()

    val passphraseGeneratedValidated : LiveData<Void?> get() = _passphraseGeneratedValidated
    private val _passphraseGeneratedValidated = SingleLiveEvent<Void?>()
    val requirePassphraseGeneration : LiveData<Void?> get() = _requirePassphraseGeneration
    private val _requirePassphraseGeneration = SingleLiveEvent<Void?>()

    fun setKeyGenerated(passKey: String) {
        _keyGenerated.value = passKey
    }

    fun validateKeyGenerated() {
        _keyGeneratedValidated.call()
    }

    fun validatePasswordGenerated() {
        _passwordGeneratedValidated.call()
    }

    fun validatePassphraseGenerated() {
        _passphraseGeneratedValidated.call()
    }

    fun requireKeyGeneration() {
        _requireKeyGeneration.call()
    }

    fun requirePasswordGeneration() {
        _requirePasswordGeneration.call()
    }

    fun requirePassphraseGeneration() {
        _requirePassphraseGeneration.call()
    }
}