package com.kunzisoft.keepass.tasks

import androidx.lifecycle.ViewModel
import com.kunzisoft.keepass.database.ProgressMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProgressTaskViewModel: ViewModel() {

    private val mProgressMessageState = MutableStateFlow(ProgressMessage(""))
    val progressMessageState: StateFlow<ProgressMessage> = mProgressMessageState

    private val mProgressTaskState = MutableStateFlow<ProgressTaskState>(ProgressTaskState.Stop)
    val progressTaskState: StateFlow<ProgressTaskState> = mProgressTaskState

    fun update(value: ProgressMessage) {
        mProgressMessageState.value = value
    }

    fun start(value: ProgressMessage) {
        mProgressTaskState.value = ProgressTaskState.Start
        update(value)
    }

    fun stop() {
        mProgressTaskState.value = ProgressTaskState.Stop
    }

    sealed class ProgressTaskState {
        object Start: ProgressTaskState()
        object Stop: ProgressTaskState()
    }
}