package com.tokenautocomplete

import android.annotation.SuppressLint
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
@SuppressLint("ParcelCreator")
open class TagTokenizer constructor(private val tagPrefixes: List<Char>) : Tokenizer {

    internal constructor() : this(listOf<Char>('@', '#'))

    @Suppress("MemberVisibilityCanBePrivate")
    protected fun isTokenTerminator(character: Char): Boolean {
        //Allow letters, numbers and underscores
        return !Character.isLetterOrDigit(character) && character != '_'
    }

    override fun containsTokenTerminator(charSequence: CharSequence): Boolean {
        for (element in charSequence) {
            if (isTokenTerminator(element)) {
                return true
            }
        }
        return false
    }

    override fun findTokenRanges(charSequence: CharSequence, start: Int, end: Int): List<Range> {
        val result = ArrayList<Range>()
        if (start == end) {
            //Can't have a 0 length token
            return result
        }
        var tokenStart = Int.MAX_VALUE
        for (cursor in start until end) {
            val character = charSequence[cursor]

            //Either this is a terminator, or we contain some content and are at the end of input
            if (isTokenTerminator(character)) {
                //Is there some token content? Might just be two terminators in a row
                if (cursor - 1 > tokenStart) {
                    result.add(Range(tokenStart, cursor))
                }

                //mark that we don't have a candidate token start any more
                tokenStart = Int.MAX_VALUE
            }

            //Set tokenStart when we hit a tag prefix
            if (tagPrefixes.contains(character)) {
                tokenStart = cursor
            }
        }
        if (end > tokenStart) {
            //There was unterminated text after a start of token
            result.add(Range(tokenStart, end))
        }
        return result
    }

    override fun wrapTokenValue(unwrappedTokenValue: CharSequence): CharSequence {
        return unwrappedTokenValue
    }

}