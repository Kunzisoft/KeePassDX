/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Proof-of-concept native coordinator client for KeePassDX.
 * NO application lifecycle hooks are wired in yet: do not use with real KDBX files.
 */
package com.kunzisoft.keepass.coordination

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Participates in the same HTTPS lease/revision protocol as the KeePassXC proof of concept.
 * The caller owns the encrypted KDBX bytes and must preserve any dirty local copy on failure.
 * All methods except close() are blocking: call from a background dispatcher, NEVER the UI thread.
 * A session's lease is held until close(), including across successful publications.
 */
class KdbxCoordinatorClient(
    private val baseUrl: String,
    private val databaseId: String,
    private val accessToken: String,
    private val owner: String
) : AutoCloseable {
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "kdbx-lease-renewal").apply { isDaemon = true }
    }
    private val mutex = Any()
    private val lost = AtomicBoolean(false)
    private var lease: String? = null
    private var revision: String? = null

    init {
        require(Regex("[a-z0-9_-]{1,96}").matches(databaseId)) { "Invalid database id" }
        require(accessToken.length >= 32 && '\n' !in accessToken && '\r' !in accessToken)
        require(Regex("[A-Za-z0-9 ._-]{1,64}").matches(owner)) { "Invalid owner label" }
        val uri = URI(baseUrl)
        require(uri.scheme == "https" ||
            (uri.scheme == "http" && (uri.host == "127.0.0.1" || uri.host == "localhost"))) {
            "HTTPS required (only localhost may use HTTP for tests)"
        }
        require(uri.userInfo == null && uri.query == null && uri.fragment == null)
    }

    class BusyException : IOException("Database is currently being edited elsewhere")
    class LostLeaseException : IOException("Exclusive editing lease was lost")
    class StaleRevisionException : IOException("Remote database revision changed")

    private data class Response(val status: Int, val payload: ByteArray, val etag: String?)
    private val endpoint get() = baseUrl.trimEnd('/') + "/v1/databases/" + databaseId

    private fun request(
        method: String, path: String, body: ByteArray? = null,
        leaseValue: String? = null, ifMatch: String? = null
    ): Response {
        val connection = URI(endpoint + path).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 40_000
            connection.instanceFollowRedirects = false // Never leak bearer or lease to another origin.
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json, application/octet-stream")
            connection.setRequestProperty("Cache-Control", "no-store")
            if (leaseValue != null) connection.setRequestProperty("X-KDBX-Lease", leaseValue)
            if (ifMatch != null) connection.setRequestProperty("If-Match", "\"$ifMatch\"")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type",
                    if (method == "PUT") "application/octet-stream" else "application/json")
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            val status = connection.responseCode
            if (status in 300..399) throw IOException("Refusing HTTP redirect: $status")
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val payload = stream?.use { it.readBytes() } ?: byteArrayOf()
            return Response(status, payload, connection.getHeaderField("ETag"))
        } finally {
            connection.disconnect()
        }
    }

    private fun getField(response: Response, field: String): String {
        // Fields from the coordinator are machine-generated tokens and hex hashes.
        val matcher = Regex("\"" + Regex.escape(field) + "\"\\s*:\\s*\"([A-Za-z0-9_-]+)\"")
        val value = matcher.find(String(response.payload, StandardCharsets.UTF_8))?.groupValues?.get(1)
        return value ?: throw IOException("Coordinator response missing $field")
    }

    private fun requireLease(): String = synchronized(mutex) {
        if (lost.get()) throw LostLeaseException()
        lease ?: throw IOException("No editing lease")
    }

    /** A 423 response means read-only mode, not permission to edit an offline copy. */
    fun acquire() {
        synchronized(mutex) { check(lease == null) { "Already editing" } }
        val body = "{\"owner\":\"${owner.replace("\\", "\\\\").replace("\"", "\\\"")}\"}".toByteArray()
        val response = request("POST", "/lock", body)
        if (response.status == 423) throw BusyException()
        if (response.status != 200) throw IOException("Lock rejected: HTTP ${response.status}")
        val newToken = getField(response, "token")
        val newRevision = getField(response, "revision")
        if (!Regex("[a-f0-9]{64}").matches(newToken) ||
            !(newRevision == "0" || Regex("[a-f0-9]{64}").matches(newRevision))) {
            throw IOException("Invalid coordinator lock metadata")
        }
        synchronized(mutex) {
            lease = newToken
            revision = newRevision
            lost.set(false)
        }
        executor.scheduleWithFixedDelay({
            try { renew() } catch (_: Exception) { lost.set(true) }
        }, 25, 25, TimeUnit.SECONDS)
    }

    fun hasLease(): Boolean = synchronized(mutex) { lease != null && !lost.get() }

    /** Revision held by this lease, used to bind a pending recovery copy. */
    fun currentRevision(): String = synchronized(mutex) {
        if (lost.get()) throw LostLeaseException()
        revision ?: throw IOException("No editing revision")
    }

    fun downloadReadOnly(): ByteArray? {
        val response = request("GET", "/file")
        if (response.status == 404) return null
        if (response.status != 200) throw IOException("Download failed: HTTP ${response.status}")
        if (response.etag != "\"${sha256(response.payload)}\"") {
            throw IOException("Remote checksum mismatch")
        }
        return response.payload
    }

    fun download(): ByteArray? {
        requireLease()
        val expected = synchronized(mutex) { revision!! }
        val response = request("GET", "/file")
        if (response.status == 404 && expected == "0") return null
        if (response.status != 200) throw IOException("Download failed: HTTP ${response.status}")
        val calculated = sha256(response.payload)
        if (expected != calculated || response.etag != "\"$calculated\"") {
            lost.set(true)
            throw StaleRevisionException()
        }
        requireLease() // Renewal may have failed during the download.
        return response.payload
    }

    fun publish(encryptedKdbx: ByteArray): String {
        require(encryptedKdbx.isNotEmpty()) { "Refusing empty KDBX" }
        val currentLease = requireLease()
        val oldRevision = synchronized(mutex) { revision!! }
        val response = request("PUT", "/file", encryptedKdbx, currentLease, oldRevision)
        if (response.status == 409) { lost.set(true); throw LostLeaseException() }
        if (response.status == 412) { lost.set(true); throw StaleRevisionException() }
        if (response.status != 200) throw IOException("Remote save failed: HTTP ${response.status}")
        val newRevision = getField(response, "revision")
        val contentHash = sha256(encryptedKdbx)
        if (newRevision != contentHash || response.etag != "\"$contentHash\"") {
            lost.set(true)
            throw IOException("Remote save acknowledgement does not match uploaded bytes")
        }
        // It is only safe for the GUI to clear its dirty flag AFTER this returns.
        requireLease()
        synchronized(mutex) { revision = newRevision }
        return newRevision
    }

    private fun renew() {
        val token = requireLease()
        val response = request("POST", "/renew", leaseValue = token)
        if (response.status != 200) { lost.set(true); throw LostLeaseException() }
    }

    /** Always preserve unsynced bytes in the calling app before invoking close(). */
    override fun close() {
        executor.shutdownNow()
        val token = synchronized(mutex) { val t = lease; lease = null; revision = null; t }
        if (token != null) {
            try {
                val response = request("DELETE", "/lock", leaseValue = token)
                if (response.status != 204 && response.status != 409) {
                    throw IOException("Release failed: HTTP ${response.status}")
                }
            } finally { lost.set(true) }
        }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
