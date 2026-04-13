package com.kunzisoft.keepass.database.element

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.utils.readListCompat
import com.kunzisoft.keepass.utils.writeListCompat

class Tags: Parcelable {

    private val mTags = mutableListOf<Tag>()

    constructor()

    constructor(values: String): this() {
        mTags.addAll(values
            .split(DELIMITER, DELIMITER1)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { Tag(it) }
            .distinct()
        )
    }

    constructor(parcel: Parcel) : this() {
        parcel.readListCompat(mTags)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeListCompat(mTags)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun setTags(tags: Tags) {
        mTags.clear()
        mTags.addAll(tags.mTags)
    }

    fun get(position: Int): Tag {
        return mTags[position]
    }

    fun put(tag: Tag) {
        val trimmedTag = Tag(tag.name.trim(), tag.isSelected)
        if (trimmedTag.name.isNotEmpty() && !mTags.contains(trimmedTag))
            mTags.add(trimmedTag)
    }
    
    fun put(tag: String) {
        put(Tag(tag.trim()))
    }

    fun put(tags: Tags) {
        tags.mTags.forEach {
            put(it)
        }
    }

    fun contains(tag: Tag): Boolean {
        return mTags.contains(Tag(tag.name.trim()))
    }

    fun contains(tag: String): Boolean {
        return mTags.contains(Tag(tag.trim()))
    }

    fun isEmpty(): Boolean {
        return mTags.isEmpty()
    }

    fun isNotEmpty(): Boolean {
        return !isEmpty()
    }

    fun size(): Int {
        return mTags.size
    }

    fun clear() {
        mTags.clear()
    }

    fun selectAll() {
        mTags.forEach { it.isSelected = true }
    }

    fun deselectAll() {
        mTags.forEach { it.isSelected = false }
    }

    fun toStringList(): List<String> {
        return mTags.map { it.name }
    }

    override fun toString(): String {
        return toStringList().joinToString(DELIMITER.toString())
    }

    companion object CREATOR : Parcelable.Creator<Tags> {
        const val DELIMITER= ','
        const val DELIMITER1= ';'
        val DELIMITERS = listOf(',', ';')

        override fun createFromParcel(parcel: Parcel): Tags {
            return Tags(parcel)
        }

        override fun newArray(size: Int): Array<Tags?> {
            return arrayOfNulls(size)
        }
    }
}