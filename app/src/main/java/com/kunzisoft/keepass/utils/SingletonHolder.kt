package com.kunzisoft.keepass.utils

open class SingletonHolderParameter<out T, in A>(private val constructor: (A) -> T) {

    @Volatile
    private var instance: T? = null

    fun getInstance(arg: A): T {
        return when {
            instance != null -> instance!!
            else -> synchronized(this) {
                if (instance == null) instance = constructor(arg)
                instance!!
            }
        }
    }
}

open class SingletonHolder<out T>(private val constructor: () -> T) {

    @Volatile
    private var instance: T? = null

    fun getInstance(): T {
        return when {
            instance != null -> instance!!
            else -> synchronized(this) {
                if (instance == null) instance = constructor()
                instance!!
            }
        }
    }
}