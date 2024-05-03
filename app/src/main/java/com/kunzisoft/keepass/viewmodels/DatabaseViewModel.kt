package com.kunzisoft.keepass.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.crypto.EncryptionAlgorithm
import com.kunzisoft.keepass.database.crypto.kdf.KdfEngine
import com.kunzisoft.keepass.database.element.Group
import com.kunzisoft.keepass.database.element.database.CompressionAlgorithm
import com.kunzisoft.keepass.tasks.ActionRunnable

class DatabaseViewModel: ViewModel() {

    val database : LiveData<ContextualDatabase?> get() = _database
    private val _database = MutableLiveData<ContextualDatabase?>()

    val actionFinished : LiveData<ActionResult> get() = _actionFinished
    private val _actionFinished = SingleLiveEvent<ActionResult>()

    val saveDatabase : LiveData<Boolean> get() = _saveDatabase
    private val _saveDatabase = SingleLiveEvent<Boolean>()

    val mergeDatabase : LiveData<Boolean> get() = _mergeDatabase
    private val _mergeDatabase = SingleLiveEvent<Boolean>()

    val reloadDatabase : LiveData<Boolean> get() = _reloadDatabase
    private val _reloadDatabase = SingleLiveEvent<Boolean>()

    val saveName : LiveData<SuperString> get() = _saveName
    private val _saveName = SingleLiveEvent<SuperString>()

    val saveDescription : LiveData<SuperString> get() = _saveDescription
    private val _saveDescription = SingleLiveEvent<SuperString>()

    val saveDefaultUsername : LiveData<SuperString> get() = _saveDefaultUsername
    private val _saveDefaultUsername = SingleLiveEvent<SuperString>()

    val saveColor : LiveData<SuperString> get() = _saveColor
    private val _saveColor = SingleLiveEvent<SuperString>()

    val saveCompression : LiveData<SuperCompression> get() = _saveCompression
    private val _saveCompression = SingleLiveEvent<SuperCompression>()

    val removeUnlinkData : LiveData<Boolean> get() = _removeUnlinkData
    private val _removeUnlinkData = SingleLiveEvent<Boolean>()

    val saveRecycleBin : LiveData<SuperGroup> get() = _saveRecycleBin
    private val _saveRecycleBin = SingleLiveEvent<SuperGroup>()

    val saveTemplatesGroup : LiveData<SuperGroup> get() = _saveTemplatesGroup
    private val _saveTemplatesGroup = SingleLiveEvent<SuperGroup>()

    val saveMaxHistoryItems : LiveData<SuperInt> get() = _saveMaxHistoryItems
    private val _saveMaxHistoryItems = SingleLiveEvent<SuperInt>()

    val saveMaxHistorySize : LiveData<SuperLong> get() = _saveMaxHistorySize
    private val _saveMaxHistorySize = SingleLiveEvent<SuperLong>()

    val saveEncryption : LiveData<SuperEncryption> get() = _saveEncryption
    private val _saveEncryption = SingleLiveEvent<SuperEncryption>()

    val saveKeyDerivation : LiveData<SuperKeyDerivation> get() = _saveKeyDerivation
    private val _saveKeyDerivation = SingleLiveEvent<SuperKeyDerivation>()

    val saveIterations : LiveData<SuperLong> get() = _saveIterations
    private val _saveIterations = SingleLiveEvent<SuperLong>()

    val saveMemoryUsage : LiveData<SuperLong> get() = _saveMemoryUsage
    private val _saveMemoryUsage = SingleLiveEvent<SuperLong>()

    val saveParallelism : LiveData<SuperLong> get() = _saveParallelism
    private val _saveParallelism = SingleLiveEvent<SuperLong>()


    fun defineDatabase(database: ContextualDatabase?) {
        this._database.value = database
    }

    fun onActionFinished(database: ContextualDatabase,
                         actionTask: String,
                         result: ActionRunnable.Result) {
        this._actionFinished.value = ActionResult(database, actionTask, result)
    }

    fun saveDatabase(save: Boolean) {
        _saveDatabase.value = save
    }

    fun mergeDatabase(save: Boolean) {
        _mergeDatabase.value = save
    }

    fun reloadDatabase(fixDuplicateUuid: Boolean) {
        _reloadDatabase.value = fixDuplicateUuid
    }

    fun saveName(oldValue: String,
                 newValue: String,
                 save: Boolean) {
        _saveName.value = SuperString(oldValue, newValue, save)
    }

    fun saveDescription(oldValue: String,
                        newValue: String,
                        save: Boolean) {
        _saveDescription.value = SuperString(oldValue, newValue, save)
    }

    fun saveDefaultUsername(oldValue: String,
                            newValue: String,
                            save: Boolean) {
        _saveDefaultUsername.value = SuperString(oldValue, newValue, save)
    }

    fun saveColor(oldValue: String,
                  newValue: String,
                  save: Boolean) {
        _saveColor.value = SuperString(oldValue, newValue, save)
    }

    fun saveCompression(oldValue: CompressionAlgorithm,
                        newValue: CompressionAlgorithm,
                        save: Boolean) {
        _saveCompression.value = SuperCompression(oldValue, newValue, save)
    }

    fun removeUnlinkedData(save: Boolean) {
        _removeUnlinkData.value = save
    }

    fun saveRecycleBin(oldValue: Group?,
                       newValue: Group?,
                       save: Boolean) {
        _saveRecycleBin.value = SuperGroup(oldValue, newValue, save)
    }

    fun saveTemplatesGroup(oldValue: Group?,
                           newValue: Group?,
                           save: Boolean) {
        _saveTemplatesGroup.value = SuperGroup(oldValue, newValue, save)
    }

    fun saveMaxHistoryItems(oldValue: Int,
                            newValue: Int,
                            save: Boolean) {
        _saveMaxHistoryItems.value = SuperInt(oldValue, newValue, save)
    }

    fun saveMaxHistorySize(oldValue: Long,
                           newValue: Long,
                           save: Boolean) {
        _saveMaxHistorySize.value = SuperLong(oldValue, newValue, save)
    }


    fun saveEncryption(oldValue: EncryptionAlgorithm,
                       newValue: EncryptionAlgorithm,
                       save: Boolean) {
        _saveEncryption.value = SuperEncryption(oldValue, newValue, save)
    }

    fun saveKeyDerivation(oldValue: KdfEngine,
                          newValue: KdfEngine,
                          save: Boolean) {
        _saveKeyDerivation.value = SuperKeyDerivation(oldValue, newValue, save)
    }

    fun saveIterations(oldValue: Long,
                       newValue: Long,
                       save: Boolean) {
        _saveIterations.value = SuperLong(oldValue, newValue, save)
    }

    fun saveMemoryUsage(oldValue: Long,
                           newValue: Long,
                           save: Boolean) {
        _saveMemoryUsage.value = SuperLong(oldValue, newValue, save)
    }

    fun saveParallelism(oldValue: Long,
                        newValue: Long,
                        save: Boolean) {
        _saveParallelism.value = SuperLong(oldValue, newValue, save)
    }

    data class ActionResult(val database: ContextualDatabase,
                            val actionTask: String,
                            val result: ActionRunnable.Result)
    data class SuperString(val oldValue: String,
                           val newValue: String,
                           val save: Boolean)
    data class SuperInt(val oldValue: Int,
                        val newValue: Int,
                        val save: Boolean)
    data class SuperLong(val oldValue: Long,
                         val newValue: Long,
                         val save: Boolean)
    data class SuperMerge(val fixDuplicateUuid: Boolean,
                          val save: Boolean)
    data class SuperCompression(val oldValue: CompressionAlgorithm,
                                val newValue: CompressionAlgorithm,
                                val save: Boolean)
    data class SuperEncryption(val oldValue: EncryptionAlgorithm,
                               val newValue: EncryptionAlgorithm,
                               val save: Boolean)
    data class SuperKeyDerivation(val oldValue: KdfEngine,
                                  val newValue: KdfEngine,
                                  val save: Boolean)
    data class SuperGroup(val oldValue: Group?,
                          val newValue: Group?,
                          val save: Boolean)

}
