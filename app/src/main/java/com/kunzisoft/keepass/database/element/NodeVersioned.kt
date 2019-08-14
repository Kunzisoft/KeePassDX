package com.kunzisoft.keepass.database.element

interface NodeVersioned: PwNodeInterface<GroupVersioned>

/**
 * Type of available Nodes
 */
enum class Type {
    GROUP, ENTRY
}


