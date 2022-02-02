package com.tokenautocomplete

import android.annotation.SuppressLint
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import kotlinx.parcelize.Parcelize
import java.util.*

/**
 * Tokenizer with configurable array of characters to tokenize on.
 *
 * Created on 2/3/15.
 * @author mgod
 */
@Parcelize
@SuppressLint("ParcelCreator")
open class CharacterTokenizer(private val splitChar: List<Char>, private val tokenTerminator: String) : Tokenizer {
    override fun containsTokenTerminator(charSequence: CharSequence): Boolean {
        for (element in charSequence) {
            if (splitChar.contains(element)) {
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
        var tokenStart = start
        for (cursor in start until end) {
            val character = charSequence[cursor]

            //Avoid including leading whitespace, tokenStart will match the cursor as long as we're at the start
            if (tokenStart == cursor && Character.isWhitespace(character)) {
                tokenStart = cursor + 1
            }

            //Either this is a split character, or we contain some content and are at the end of input
            if (splitChar.contains(character) || cursor == end - 1) {
                val hasTokenContent =  //There is token content befor the current character
                    cursor > tokenStart ||  //If the current single character is valid token content, not a split char or whitespace
                            cursor == tokenStart && !splitChar.contains(character)
                if (hasTokenContent) {
                    //There is some token content
                    //Add one to range end as the end of the ranges is not inclusive
                    result.add(Range(tokenStart, cursor + 1))
                }
                tokenStart = cursor + 1
            }
        }
        return result
    }

    override fun wrapTokenValue(unwrappedTokenValue: CharSequence): CharSequence {
        val wrappedText: CharSequence = unwrappedTokenValue.toString() + tokenTerminator
        return if (unwrappedTokenValue is Spanned) {
            val sp = SpannableString(wrappedText)
            TextUtils.copySpansFrom(
                unwrappedTokenValue, 0, unwrappedTokenValue.length,
                Any::class.java, sp, 0
            )
            sp
        } else {
            wrappedText
        }
    }
}