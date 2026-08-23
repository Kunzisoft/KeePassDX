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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.utils.clear
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for generating keys.
 */
class KeyGeneratorViewModel : ViewModel() {

    private val _keyGenerated = MutableStateFlow<CharArray?>(null)

    /**
     * StateFlow representing the generated key.
     */
    val keyGenerated: StateFlow<CharArray?> = _keyGenerated.asStateFlow()

    private val _keyGeneratedValidated = MutableSharedFlow<Unit>()

    /**
     * SharedFlow triggered when the generated key is validated.
     */
    val keyGeneratedValidated: SharedFlow<Unit> = _keyGeneratedValidated.asSharedFlow()

    private val _requireKeyGeneration = MutableSharedFlow<Unit>()

    /**
     * SharedFlow triggered when a key generation is required.
     */
    val requireKeyGeneration: SharedFlow<Unit> = _requireKeyGeneration.asSharedFlow()

    private val _passwordGeneratedValidated = MutableSharedFlow<Unit>()

    /**
     * SharedFlow triggered when the generated password is validated.
     */
    val passwordGeneratedValidated: SharedFlow<Unit> = _passwordGeneratedValidated.asSharedFlow()

    private val _requirePasswordGeneration = MutableSharedFlow<Unit>()

    /**
     * SharedFlow triggered when a password generation is required.
     */
    val requirePasswordGeneration: SharedFlow<Unit> = _requirePasswordGeneration.asSharedFlow()

    private val _passphraseGeneratedValidated = MutableSharedFlow<Unit>()

    /**
     * SharedFlow triggered when the generated passphrase is validated.
     */
    val passphraseGeneratedValidated: SharedFlow<Unit> = _passphraseGeneratedValidated.asSharedFlow()

    private val _requirePassphraseGeneration = MutableSharedFlow<Unit>()

    /**
     * SharedFlow triggered when a passphrase generation is required.
     */
    val requirePassphraseGeneration: SharedFlow<Unit> = _requirePassphraseGeneration.asSharedFlow()

    /**
     * Set the generated key.
     */
    fun setKeyGenerated(value: CharArray?) {
        _keyGenerated.value = value?.copyOf()
    }

    /**
     * Validate the generated key.
     */
    fun validateKeyGenerated() {
        viewModelScope.launch {
            _keyGeneratedValidated.emit(Unit)
        }
    }

    /**
     * Validate the generated password.
     */
    fun validatePasswordGenerated() {
        viewModelScope.launch {
            _passwordGeneratedValidated.emit(Unit)
        }
    }

    /**
     * Validate the generated passphrase.
     */
    fun validatePassphraseGenerated() {
        viewModelScope.launch {
            _passphraseGeneratedValidated.emit(Unit)
        }
    }

    /**
     * Require a key generation.
     */
    fun requireKeyGeneration() {
        viewModelScope.launch {
            _requireKeyGeneration.emit(Unit)
        }
    }

    /**
     * Require a password generation.
     */
    fun requirePasswordGeneration() {
        viewModelScope.launch {
            _requirePasswordGeneration.emit(Unit)
        }
    }

    /**
     * Require a passphrase generation.
     */
    fun requirePassphraseGeneration() {
        viewModelScope.launch {
            _requirePassphraseGeneration.emit(Unit)
        }
    }

    override fun onCleared() {
        super.onCleared()
        _keyGenerated.value?.clear()
    }
}
