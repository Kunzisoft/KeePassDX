package com.kunzisoft.keepass.services

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.*
import android.nfc.tech.*
import android.util.Log
import java.io.IOException

class NfcService {
    companion object {
        private val TAG = NfcService::class.java.name
    }

    private var adapter: NfcAdapter? = null

    private fun getAdapter(context: Context?) = adapter
        ?: context?.getSystemService(Context.NFC_SERVICE)?.let { nfcManager ->
            (nfcManager as NfcManager).defaultAdapter.also { adapter = it }
        }

    fun isSupported(context: Context): Boolean = null != getAdapter(context)

    val isEnabled: Boolean get() = true == adapter?.isEnabled

    fun enableDispatch(activity: Activity?, tagActivity: Class<*>?) = try {
        getAdapter(activity)?.enableForegroundDispatch(activity, PendingIntent.getActivity(activity, 0,
            Intent(activity, tagActivity).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE),
            //todo-op!!! check/test. Comment from original code: PendingIntent.FLAG_IMMUTABLE doesn't work, must maybe be mutable
            null, null)
    } catch (e: Throwable) { //IllegalStateException
        Log.e(TAG, "NFC error: Dispatch enable", e)
    }

    fun disableDispatch(activity: Activity?) = try {
        adapter?.disableForegroundDispatch(activity)
    } catch (e: Throwable) { //IllegalStateException
        Log.e(TAG, "NFC error: Dispatch disable", e)
    }

    /***/

    var nfcTag: NfcTag? = null

    fun readTag(intent: Intent?, onTag: (NfcTag?, String?, String?) -> Unit): Boolean {
        val tag: Tag = intent?.getParcelableExtra(NfcAdapter.EXTRA_TAG) ?: return false
        nfcTag?.close()
        try {
            var nfcTagData = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.let { messages ->
                StringBuilder().let { stringBuilder ->
                    messages.forEach { message ->
                        (message as NdefMessage).records.forEach {
                            //if (it.tnf == NdefRecord.TNF_MIME_MEDIA) //todo-op!!! ???
                            stringBuilder.append(String(it.payload))
                        }
                    }
                }.toString()
            }
            if (null == nfcTagData) nfcTagData = ""
            //nfcTagData = (intent.extras?.get(NfcAdapter.EXTRA_ID)?.toString() ?: "") +
            //        (intent.extras?.get(NfcAdapter.EXTRA_TAG)?.toString() ?: "") +
            //        (intent.extras?.get(NfcAdapter.EXTRA_ADAPTER_STATE)?.toString() ?: "") +
            //        (intent.extras?.get(NfcAdapter.EXTRA_AID)?.toString() ?: "") +
            //        (intent.extras?.get(NfcAdapter.EXTRA_DATA)?.toString() ?: "") +
            //        (intent.extras?.get(NfcAdapter.EXTRA_PREFERRED_PAYMENT_CHANGED_REASON)?.toString() ?: "") +
            //        (intent.extras?.get(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY)?.toString() ?: "") +
            //        (intent.extras?.get(NfcAdapter.EXTRA_SECURE_ELEMENT_NAME)?.toString() ?: "") +
            //        (intent.extras?.get(NfcAdapter.EXTRA_NDEF_MESSAGES)?.toString() ?: "")
            intent.extras?.keySet()
                //?.filter { it != NfcAdapter.EXTRA_TAG && it != NfcAdapter.EXTRA_ID }
                ?.forEach { key ->
                    intent.extras?.get(key).let { extra ->
                        (if (extra is ByteArray) NfcTag.bytesToHexString(extra) ?: ""
                        else extra.toString()
                                ).let { if (it.isNotBlank()) nfcTagData += "\n$key:$it" }
                    }
                }
            if (nfcTagData.isBlank()) nfcTagData = null
            nfcTag = NfcTag(tag, nfcTagData)
            onTag(nfcTag, nfcTagData, null)
        } catch (e: Throwable) { //FormatException
            Log.e(TAG, "", e)
            nfcTag = null
            onTag(null, null, e.message)
        }
        return true
    }
}

class NfcTag(val tag: Tag, val data: String?) {
    companion object {
        private val TAG = NfcTag::class.java.name

        fun bytesToHexString(src: ByteArray): String? = if (src.isEmpty()) null else
            StringBuilder().also { for (b in src) it.append(String.format("%02X", b)) }.toString()
    }

    fun details(): String {
        return  "$tag\n" +
                //"tech: ${tag.techList.joinToString()}\n" +
                "id: $tagId\n" +
                "tag.describeContents: ${tag.describeContents()}\n" +
                "dataSize: ${getDataSize()}\n" +
                "maxSize: $maxSize\n" +
                "freeSize: ${getFreeSize()}\n" +
                "isWriteProtected: $isWriteProtected\n" +
                "canMakeReadOnly: $canMakeReadOnly\n" +
                "data: $data"
    }

    val tagId: String?
        get() {
            return bytesToHexString(tag.id)
        }

    fun getDataSize(): Int? = data?.length

    class Tech {
        var isoDep: IsoDep? = null
        var mifareClassic: MifareClassic? = null
        var mifareUltralight: MifareUltralight? = null
        var ndef: Ndef? = null
        var ndefFormatable: NdefFormatable? = null
        var nfcA: NfcA? = null
        var nfcB: NfcB? = null
        var nfcBarcode: NfcBarcode? = null
        var nfcF: NfcF? = null
        var nfcV: NfcV? = null
    }
    private val tech = Tech()
    init {
        Log.d(TAG, "NFC tag: ${details()}")
        if (tag.techList.contains(IsoDep::class.java.canonicalName)) tech.isoDep = IsoDep.get(tag)
        if (tag.techList.contains(MifareClassic::class.java.canonicalName)) tech.mifareClassic = MifareClassic.get(tag)
        if (tag.techList.contains(MifareUltralight::class.java.canonicalName)) tech.mifareUltralight = MifareUltralight.get(tag)
        if (tag.techList.contains(Ndef::class.java.canonicalName)) tech.ndef = Ndef.get(tag)
        if (tag.techList.contains(NdefFormatable::class.java.canonicalName)) tech.ndefFormatable = NdefFormatable.get(tag)
        if (tag.techList.contains(NfcA::class.java.canonicalName)) tech.nfcA = NfcA.get(tag)
        if (tag.techList.contains(NfcB::class.java.canonicalName)) tech.nfcB = NfcB.get(tag)
        if ( //Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 &&
            tag.techList.contains(NfcBarcode::class.java.canonicalName)) tech.nfcBarcode = NfcBarcode.get(tag)
        if (tag.techList.contains(NfcF::class.java.canonicalName)) tech.nfcF = NfcF.get(tag)
        if (tag.techList.contains(NfcV::class.java.canonicalName)) tech.nfcV = NfcV.get(tag)
    }
    //private val tagTech: TagTechnology?
    //    get() = tech.isoDep
    //        ?: tech.mifareClassic
    //        ?: tech.mifareUltralight
    //        ?: tech.ndef
    //        ?: tech.ndefFormatable
    //        ?: tech.nfcA
    //        ?: tech.nfcB
    //        ?: tech.nfcBarcode
    //        ?: tech.nfcF
    //        ?: tech.nfcV

    private val ndef: Ndef? = tech.ndef
    val maxSize = ndef?.maxSize
    val isWriteProtected = true != ndef?.isWritable
    val canMakeReadOnly = true == ndef?.canMakeReadOnly()

    private val ndefFormatable: NdefFormatable? = tech.ndefFormatable
    val isNdefSupported = ndef != null || ndefFormatable == null

    private fun getFreeSize(): Int? {
        val dataSize = getDataSize()
        return if (dataSize == null || maxSize == null) null else maxSize - dataSize
    }

    @Throws(IOException::class)
    fun close() {
        //tagTech?.close()
        tech.isoDep?.close()
        tech.mifareClassic?.close()
        tech.mifareUltralight?.close()
        tech.ndef?.close()
        tech.ndefFormatable?.close()
        tech.nfcA?.close()
        tech.nfcB?.close()
        tech.nfcBarcode?.close()
        tech.nfcF?.close()
        tech.nfcV?.close()
    }

    @Throws(IOException::class, FormatException::class)
    fun writeData(message: NdefMessage, setWriteProtection: Boolean = false): Boolean {
        ndef?.let { ndef ->
            ndef.connect() //tagTech?.connect()
            if (ndef.isConnected) { //tagTech?.isConnected
                ndef.writeNdefMessage(message)
                if (setWriteProtection && ndef.canMakeReadOnly()) ndef.makeReadOnly()
                ndef.close() //tagTech?.close()
                return true
            }
        } ?: ndefFormatable?.let { ndefFormatable ->
            ndefFormatable.connect() //tagTech?.connect()
            if (ndefFormatable.isConnected) { //tagTech?.isConnected
                ndefFormatable.format(message)
                ndefFormatable.close() //tagTech?.close()
                return true
            }
        } ?: throw FormatException("Tag does not support ndef")
        return false
    }

    //todo-op: write
    // https://stackoverflow.com/questions/70264201/how-to-write-to-a-mifare-ultralight-card-on-android-studio-java
    // ...I would ignore most guides out there on the internet as they use the older enableForgroundDispatch API...
    // ...another example but shows how to use NfcA transceive...
    // https://stackoverflow.com/a/64921434/2373819
    // 1 ...To get reliable writing to NFC with Android you should use the newer and much better enableReaderMode API...
    // https://stackoverflow.com/a/59397667/2373819
    // 2 ...I would not use the newIntent method as this is very unreliable for writing data, I would use the enableReaderMode if you are targeting a high enough version of Android...
    // https://www.mifare.net/en/products/tools/taplinx/
    // 3 ...MIFARE SDK is now TapLinx. This means more supported products, more features, and a redesigned open API...
    // https://gitlab.com/marc.farssac.busquets/nxp-taplinx-android-nfc-reader-writer
    // https://github.com/grspy/ulev1plus
    // https://github.com/mickychanhk/Read-Mifare-Plus-S-Content
}
