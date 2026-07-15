/*
 * Copyright 2024 Jeremy Jamet / Kunzisoft.
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
 */

package com.kunzisoft.keepass.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.DatabaseTaskProvider
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.utils.CsvReader
import com.kunzisoft.keepass.utils.EntryMappingIterator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class CsvImportViewModel(application: Application) : AndroidViewModel(application) {

    val headers = MutableLiveData<List<String>>()
    val mapping = MutableLiveData<MutableMap<Int, EntryMappingIterator.FieldType>>()

    private val databaseTaskProvider = DatabaseTaskProvider(application)
    private var csvReader: CsvReader? = null

    fun setFile(uri: Uri) {
        csvReader?.close()
        csvReader = null
        viewModelScope.launch(Dispatchers.IO) {
            val reader = try {
                CsvReader(getApplication<Application>().contentResolver, uri)
            } catch (e: IOException) {
                return@launch
            }
            if (!reader.hasNext()) {
                reader.close()
                return@launch
            }
            val headerRecord = reader.next()
            val headerList = headerRecord.map { String(it) }
            val newMapping = mutableMapOf<Int, EntryMappingIterator.FieldType>()
            
            // Chrome password CSV fields:
            // name,url,username,password,note
            
            // Firefox password CSV fields:
            // url,username,password,httpRealm,formActionOrigin,guid,timeCreated,timeLastUsed,timePasswordChanged
            
            headerList.forEachIndexed { index, s ->
                newMapping[index] = when (s.trim().lowercase()) {
                    "name", "title"             -> EntryMappingIterator.FieldType.TITLE
                    "url"                       -> EntryMappingIterator.FieldType.URL
                    "username", "login", "user" -> EntryMappingIterator.FieldType.USERNAME
                    "password"                  -> EntryMappingIterator.FieldType.PASSWORD
                    "notes", "note"             -> EntryMappingIterator.FieldType.NOTES
                    else                        -> EntryMappingIterator.FieldType.IGNORE
                }
            }
            withContext(Dispatchers.Main) {
                csvReader = reader
                headers.value = headerList
                mapping.value = newMapping
            }
        }
    }

    fun startImport(database: ContextualDatabase, parent: Group) {
        val reader = csvReader ?: return
        val currentMapping = mapping.value ?: return
        csvReader = null
        databaseTaskProvider.startDatabaseImport(
            EntryMappingIterator(reader, currentMapping, database),
            parent.nodeId,
            true
        )
    }

    override fun onCleared() {
        super.onCleared()
        csvReader?.close()
        csvReader = null
    }
}
