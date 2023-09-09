package com.kunzisoft.keepass.autofill

import android.app.assist.AssistStructure

data class AutofillComponent(val assistStructure: AssistStructure,
                             val compatInlineSuggestionsRequest: CompatInlineSuggestionsRequest?)