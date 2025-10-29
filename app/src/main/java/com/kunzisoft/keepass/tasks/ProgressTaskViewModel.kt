package com.kunzisoft.keepass.tasks

import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.database.ProgressMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ProgressTaskViewModel: ViewModel() {

    private val mProgressTaskState = MutableStateFlow<ProgressTaskState>(ProgressTaskState.Hide)
    val progressTaskState: StateFlow<ProgressTaskState> = mProgressTaskState

    fun show(value: ProgressMessage) {
        mProgressTaskState.update { currentState ->
            ProgressTaskState.Show(value)
        }
    }

    fun hide() {
        mProgressTaskState.value = ProgressTaskState.Hide
    }

    sealed class ProgressTaskState {
        data class Show(val value: ProgressMessage): ProgressTaskState()
        object Hide: ProgressTaskState()
    }
}