/*
 * Copyright 2024 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.kunzisoft.keepass.utils

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Secure RFC 4180 CSV reader that reads the file at [uri] entirely into an owned [ByteArray],
 * then iterates logical records one at a time.
 *
 * Each record is returned as an [Array]<[CharArray]> where every element is a single field.
 * Quoted fields (including those containing embedded newline characters) are handled correctly.
 *
 * Security properties:
 * - The previous record's [CharArray]s are zeroed (filled with `\u0000`) on every call to [next].
 * - All internal scratch memory and the owned file buffer are zeroed on [close].
 * - Callers that must retain a field value beyond the next [next] call must [CharArray.copyOf]
 *   the array before advancing the iterator. In particular, password fields should be copied
 *   as [CharArray] rather than converted to [String] so the secret does not linger on the heap.
 *
 * Use with `use { }` or call [close] explicitly when done.
 *
 * Fields larger than [MAX_FIELD_BYTES] bytes are silently truncated.
 */
class CsvReader(contentResolver: ContentResolver, uri: Uri) : CloseableIterator<Array<CharArray>> {

    /** Entire file content, owned by this instance and zeroed on [close]. */
    private val rawBytes: ByteArray

    /** Current read position in [rawBytes]. */
    private var pos: Int = 0

    /** Scratch buffer for accumulating raw bytes of the field being decoded; reused across calls. */
    private val scratch: ByteArray = ByteArray(MAX_FIELD_BYTES)
    private var scratchLen: Int = 0

    /** The record that was returned by the most recent [next] call; zeroed before the next one. */
    private var currentRecord: Array<CharArray>? = null

    /**
     * The next record, pre-fetched during construction and after every [next] call,
     * so that [hasNext] can inspect it without consuming it.
     */
    private var peeked: Array<CharArray>?

    init {
        rawBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Cannot open URI: $uri")
        // Skip the UTF-8 BOM (EF BB BF) if present.
        if (rawBytes.size >= 3
            && rawBytes[0] == 0xEF.toByte()
            && rawBytes[1] == 0xBB.toByte()
            && rawBytes[2] == 0xBF.toByte()
        ) {
            pos = 3
        }
        peeked = parseNextRecord()
    }

    // -------------------------------------------------------------------------
    // Iterator / Closeable API
    // -------------------------------------------------------------------------

    /** Returns `true` while there is at least one more non-empty record to read. */
    override fun hasNext(): Boolean = peeked != null

    /**
     * Returns the next record and advances the iterator.
     *
     * The returned [Array]<[CharArray]> is owned by the caller until the next call to [next],
     * at which point every [CharArray] in it is zeroed. Copy any field you need to retain.
     */
    override fun next(): Array<CharArray> {
        if (!hasNext()) throw NoSuchElementException()
        // Zero the record we handed out last time.
        zeroRecord(currentRecord)
        currentRecord = peeked
        peeked = parseNextRecord()
        return currentRecord!!
    }

    /**
     * Zeroes all retained [CharArray]s, the scratch buffer, and the owned file bytes,
     * then releases references so the GC can reclaim memory promptly.
     */
    override fun close() {
        zeroRecord(currentRecord)
        zeroRecord(peeked)
        currentRecord = null
        peeked = null
        rawBytes.fill(0)
        scratch.fill(0)
        scratchLen = 0
    }

    // -------------------------------------------------------------------------
    // Parsing internals
    // -------------------------------------------------------------------------

    /** Fills every element of [record] with the null character. */
    private fun zeroRecord(record: Array<CharArray>?) {
        record?.forEach { it.fill('\u0000') }
    }

    /** Appends a single byte to [scratch], silently truncating if the buffer is full. */
    private fun appendScratch(b: Int) {
        if (scratchLen < scratch.size) {
            scratch[scratchLen++] = b.toByte()
        }
    }

    /**
     * Decodes [scratch][0..[scratchLen]) as UTF-8 into a fresh [CharArray], appends it to
     * [fields], then zeros and resets the scratch buffer.
     */
    private fun flushField(fields: MutableList<CharArray>) {
        fields.add(decodeUtf8(scratch, scratchLen))
        scratch.fill(0, 0, scratchLen)
        scratchLen = 0
    }

    /**
     * Advances past blank lines and returns the next non-empty logical record,
     * or `null` when the file is exhausted.
     */
    private fun parseNextRecord(): Array<CharArray>? {
        while (pos < rawBytes.size) {
            val record = parseRecord() ?: return null
            if (record.any { it.isNotEmpty() }) return record
            // All fields empty (blank line) — zero and skip.
            zeroRecord(record)
        }
        return null
    }

    /**
     * Parses exactly one logical CSV record starting at [pos], advancing [pos] past the
     * terminating line-ending (or to EOF).  Returns `null` only when already at EOF.
     *
     * Quoted fields may span multiple physical lines; the RFC 4180 double-quote escape (`""`)
     * is handled correctly.
     */
    private fun parseRecord(): Array<CharArray>? {
        if (pos >= rawBytes.size) return null

        val fields = mutableListOf<CharArray>()
        var inQuotes = false

        while (pos < rawBytes.size) {
            val b = rawBytes[pos].toInt() and 0xFF

            when {
                // Inside a quoted field: check for escaped double-quote ("").
                inQuotes && b == DQUOTE -> {
                    val lookahead = if (pos + 1 < rawBytes.size) rawBytes[pos + 1].toInt() and 0xFF else -1
                    if (lookahead == DQUOTE) {
                        // Escaped quote: emit one literal double-quote character.
                        appendScratch(DQUOTE)
                        pos += 2
                    } else {
                        // Closing quote of a quoted field.
                        inQuotes = false
                        pos++
                    }
                }
                // Opening quote of a quoted field.
                !inQuotes && b == DQUOTE -> {
                    inQuotes = true
                    pos++
                }
                // Field delimiter.
                !inQuotes && b == COMMA -> {
                    flushField(fields)
                    pos++
                }
                // Record delimiter: CR, LF, or CR+LF.
                !inQuotes && (b == CR || b == LF) -> {
                    flushField(fields)
                    pos++
                    // Consume the LF of a CR+LF pair.
                    if (b == CR && pos < rawBytes.size && (rawBytes[pos].toInt() and 0xFF) == LF) {
                        pos++
                    }
                    return fields.toTypedArray()
                }
                // Any other byte, including newlines inside a quoted field.
                else -> {
                    appendScratch(b)
                    pos++
                }
            }
        }

        // Reached EOF without a trailing line-ending — flush the last field.
        flushField(fields)
        return fields.toTypedArray()
    }

    /**
     * Decodes [len] bytes from the start of [bytes] as UTF-8 and returns a fresh [CharArray].
     * Returns an empty [CharArray] for zero-length input.
     *
     * [Charset.decode] allocates its own internal [java.nio.CharBuffer] holding a full copy of
     * the decoded plaintext; that buffer is not cleared by the JDK, so once the result has been
     * copied out, its backing array is explicitly zeroed here to avoid leaving an extra unwiped
     * copy of the secret on the heap.
     */
    private fun decodeUtf8(bytes: ByteArray, len: Int): CharArray {
        if (len == 0) return CharArray(0)
        val bb = ByteBuffer.wrap(bytes, 0, len)
        val cb = StandardCharsets.UTF_8.decode(bb)
        val result = CharArray(cb.remaining())
        cb.get(result)
        if (cb.hasArray()) {
            cb.array().fill('\u0000')
        }
        return result
    }

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    companion object {
        /** Maximum bytes accepted per field; content beyond this limit is silently dropped. */
        private const val MAX_FIELD_BYTES = 65_536

        private const val DQUOTE = '"'.code
        private const val COMMA = ','.code
        private const val CR    = '\r'.code
        private const val LF    = '\n'.code
    }
}
