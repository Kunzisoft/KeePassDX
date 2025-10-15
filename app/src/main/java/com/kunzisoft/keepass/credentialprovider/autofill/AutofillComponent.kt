package com.kunzisoft.keepass.credentialprovider.autofill

import android.app.assist.AssistStructure

data class AutofillComponent(
    val assistStructure: AssistStructure,
    val compatInlineSuggestionsRequest: CompatInlineSuggestionsRequest?
)