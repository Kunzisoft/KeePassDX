package com.kunzisoft.keepass.autofill

import android.app.assist.AssistStructure
import android.view.inputmethod.InlineSuggestionsRequest

data class AutofillComponent(val assistStructure: AssistStructure,
                             val inlineSuggestionsRequest: InlineSuggestionsRequest?)