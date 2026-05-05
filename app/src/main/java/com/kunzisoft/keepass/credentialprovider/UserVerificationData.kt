package com.kunzisoft.keepass.credentialprovider

import com.kunzisoft.keepass.database.ContextualDatabase
import com.kunzisoft.keepass.database.element.node.NodeId
import com.kunzisoft.keepass.model.FieldProtection

data class UserVerificationData(
    val actionType: UserVerificationActionType,
    val database: ContextualDatabase? = null,
    val entryId: NodeId<*>? = null,
    val fieldProtection: FieldProtection? = null,
    val preferenceKey: String? = null,
    val originName: String? = null
)

enum class UserVerificationActionType {
    LAUNCH_AUTHENTICATION_CEREMONY,
    SHOW_PROTECTED_FIELD,
    COPY_PROTECTED_FIELD,
    EDIT_ENTRY,
    EDIT_DATABASE_SETTING,
    MERGE_FROM_DATABASE,
    SAVE_DATABASE_COPY_TO
}