package com.kunzisoft.keepass.database.element

interface NodeVersioned: PwNodeInterface<GroupVersioned> {

    val nodeId: PwNodeId<*>?

    val nodePositionInParent: Int
        get() {
            parent?.getChildren(true)?.let { children ->
                children.forEachIndexed { index, nodeVersioned ->
                    if (nodeVersioned.nodeId == this.nodeId)
                        return index
                }
            }
            return -1
        }
}

/**
 * Type of available Nodes
 */
enum class Type {
    GROUP, ENTRY
}


