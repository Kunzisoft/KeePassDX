package com.kunzisoft.keepass.database.element

import android.os.Parcel
import android.os.Parcelable

class Tags: Parcelable {

    private val mTags = ArrayList<String>()

    constructor()

    constructor(values: String): this() {
        mTags.addAll(values.split(';'))
    }

    constructor(parcel: Parcel) : this() {
        parcel.readStringList(mTags)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeStringList(mTags)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setTags(tags: Tags) {
        mTags.clear()
        mTags.addAll(tags.mTags)
    }

    fun get(position: Int): String {
        return mTags[position]
    }

    fun put(tag: String) {
        if (!mTags.contains(tag))
            mTags.add(tag)
    }

    fun isEmpty(): Boolean {
        return mTags.isEmpty()
    }

    fun size(): Int {
        return mTags.size
    }

    fun clear() {
        mTags.clear()
    }

    override fun toString(): String {
        return mTags.joinToString(";")
    }

    companion object CREATOR : Parcelable.Creator<Tags> {
        override fun createFromParcel(parcel: Parcel): Tags {
            return Tags(parcel)
        }

        override fun newArray(size: Int): Array<Tags?> {
            return arrayOfNulls(size)
        }
    }
}