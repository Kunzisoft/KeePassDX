package com.kunzisoft.keepass.services

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.*
import android.nfc.tech.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.InputType
import android.util.Log
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import javax.crypto.*
import javax.crypto.spec.DESedeKeySpec
import javax.crypto.spec.IvParameterSpec
import kotlin.experimental.and
import kotlin.experimental.or

class NfcService(private val packageName: String, private val onError: ((String, Throwable?) -> Unit)? = null) {
    companion object {
        private val TAG = NfcService::class.java.name

        @Suppress("SpellCheckingInspection")
        private fun isEmulatorProbably() = // From device-info plugin, Flutter/Google
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                    || Build.MANUFACTURER.contains("Genymotion")
                    || Build.FINGERPRINT.startsWith("generic") || Build.FINGERPRINT.startsWith("unknown")
                    || Build.HARDWARE.contains("goldfish") || Build.HARDWARE.contains("ranchu")
                    || Build.MODEL.contains("google_sdk") || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.PRODUCT.contains("sdk_google") || Build.PRODUCT.contains("google_sdk")
                    || Build.PRODUCT.contains("sdk") || Build.PRODUCT.contains("sdk_x86")
                    || Build.PRODUCT.contains("sdk_gphone64_arm64") || Build.PRODUCT.contains("vbox86p")
                    || Build.PRODUCT.contains("emulator") || Build.PRODUCT.contains("simulator")

        val isDebug = isEmulatorProbably() // debug in emulator //todo-op!!! disable
        private const val withDump: Boolean = true //todo-op!!! disable
        fun isSupported(context: Context): Boolean = isDebug || NfcService(context.packageName).isSupported(context)
    }

    // localize
    private fun errCatchEnable() = NfcErr("Start listen for Tag failed", TAG, onError)
    private fun errCatchDisable() = NfcErr("Stop failed", TAG, onError)
    private fun errCatchTagReader() = NfcErr("Read Tag failed", TAG, onError)
    private fun errCatchTagIntent() = errCatchTagReader()
    private fun errCatchTagDebug() = NfcErr("Debug Tag", TAG, onError)

    private var adapter: NfcAdapter? = null
    fun isSupported(context: Context): Boolean = isDebug || null != getAdapter(context)
    val isEnabled: Boolean get() = true == adapter?.isEnabled

    private fun getAdapter(context: Context?) =
        adapter ?: context?.getSystemService(Context.NFC_SERVICE)?.let { nfcManager ->
            (nfcManager as NfcManager).defaultAdapter.also { adapter = it }
        }

    fun enable(activity: FragmentActivity?, tagActivity: Class<*>?, onTag: (NfcTag) -> Unit) {
        fun enableDispatch() {
            getAdapter(activity)?.enableForegroundDispatch(activity, PendingIntent.getActivity(activity, 0,
                Intent(activity, tagActivity).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE),
                null, null)
            NfcErr("NFC: Dispatch mode", TAG).log()
        }

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
                enableDispatch()
            else try {
                getAdapter(activity)?.enableReaderMode(activity, { tag ->
                    activity?.lifecycleScope?.launch(Dispatchers.Main) {
                        if (null != tag) {
                            val nfcTag = tagGet(tag)
                            try {
                                onTag(nfcTag)
                            } catch (e: Throwable) {
                                errCatchTagReader().errorCb(e)
                            }
                        }
                    }
                }, //NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK + // required
                    //NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS + // optional
                    NfcAdapter.FLAG_READER_NFC_A + NfcAdapter.FLAG_READER_NFC_B + NfcAdapter.FLAG_READER_NFC_BARCODE +
                            NfcAdapter.FLAG_READER_NFC_F + NfcAdapter.FLAG_READER_NFC_V,
                    Bundle().also {
                        it.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 5000) //todo-op??? test read protect, write pass, key authentication
                    })
                NfcErr("NFC: Reader mode", TAG).log()
            } catch (e: Throwable) {
                errCatchEnable().err(e)
                enableDispatch()
            }
        } catch (e: Throwable) {
            errCatchEnable().error(e)
        }
    }
    
    fun disable(activity: Activity?) =
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
                adapter?.disableForegroundDispatch(activity)
            else try {
                adapter?.disableReaderMode(activity)
            } catch (e: Throwable) {
                errCatchDisable().err(e)
                adapter?.disableForegroundDispatch(activity)
            }
        } catch (e: Throwable) {
            errCatchDisable().error(e)
        }

    private fun tagGet(tag: Tag?, ndefMessages: List<NdefMessage>? = null, dump: MutableList<String>? = null) =
        NfcTagUnlock(packageName, onError, tag, ndefMessages, dump ?: if (withDump || isDebug) mutableListOf() else null,
            if (!isDebug) null else NfcTag.TagType.NTag213)

    fun tagRead(intent: Intent?, onTag: (NfcTag?) -> Unit): Boolean {
        try {
            if (null == intent
                || !listOf(NfcAdapter.ACTION_NDEF_DISCOVERED, NfcAdapter.ACTION_TECH_DISCOVERED, NfcAdapter.ACTION_TAG_DISCOVERED)
                    .contains(intent.action))
                return false
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            if (null == tag && !isDebug) return false
            val dump = if (!withDump && !isDebug) null else
                mutableListOf<String>().also { list ->
                    intent.extras?.keySet()?.filter {
                        it != NfcAdapter.EXTRA_ID && it != NfcAdapter.EXTRA_TAG && it != NfcAdapter.EXTRA_NDEF_MESSAGES
                    }?.forEach { key ->
                        intent.extras?.get(key).let { extra ->
                            if (extra is ByteArray) list.add("$key: ${NfcTag.hexString(extra)}")
                            else list.add("$key: $extra")
                        }
                    }
                }
            val ndefMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.mapNotNull { it as? NdefMessage }
            val nfcTag = tagGet(tag, ndefMessages, dump)
            try {
                onTag(nfcTag)
            } catch (e: Throwable) {
                errCatchTagIntent().errorCb(e)
            }
            return true
        } catch (e: Throwable) {
            errCatchTagIntent().error(e)
            return true
        }
    }

    fun debugTap(activity: Activity?, onTag: (NfcTag) -> Unit) {
        fun ask(activity: Activity, windowToken: IBinder? = null, onYes: (String?) -> Unit) {
            val input = EditText(activity)
            input.inputType = InputType.TYPE_CLASS_TEXT
            val dialog = AlertDialog.Builder(activity)
                .setNegativeButton(activity.getString(android.R.string.cancel)) { dialog, _ -> dialog.cancel() }
                //.setOnCancelListener { onNo() }
                .setPositiveButton(activity.getString(android.R.string.ok)) { _, _ -> onYes(input.text?.toString()) }
                .setTitle("Debug NFC")
                .setMessage("Enter NFC tag data:")
                .setView(LinearLayout(activity).also { layout ->
                    layout.orientation = LinearLayout.VERTICAL
                    layout.setPadding(60, 0, 60, 0)
                    layout.addView(input)
                }).create()
            windowToken?.let {
                // for Magikeyboard: 'dialog.show() crashed InputMethodService'
                // from: https://stackoverflow.com/questions/7244637/dialog-show-crashed-inputmethodservice
                dialog.window?.also { window ->
                    window.attributes = window.attributes?.also {
                        it.token = windowToken
                        it.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
                    }
                    window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM) // FLAG_ALT_FOCUSABLE_IM = the window doesn't need input method
                }
            }
            dialog.show()
        }

        fun onYes(value: String? = null) {
            try {
                val unlockUnique = value?.let { NfcTagUnlock.unlockUnique(it.toByteArray())?.toByteArray() }
                val withIntent = false
                if (null == activity || !withIntent) {
                    val nfcTag = tagGet(null)
                    try {
                        onTag(nfcTag)
                    } catch (e: Throwable) {
                        errCatchTagDebug().errorCb(e)
                    }
                } else {
                    activity.startActivity(Intent(activity, activity::class.java)
                        .putExtra(NfcAdapter.EXTRA_ID, NfcTag.toByteArr("042FD8D2286781"))
                        .also {
                            if (null != unlockUnique) it.putParcelableArrayListExtra(NfcAdapter.EXTRA_NDEF_MESSAGES,
                                arrayListOf(NdefMessage(
                                    //NdefRecord.createApplicationRecord(packageName),
                                    NdefRecord.createMime("application/$packageName", unlockUnique),
                                )))
                        }.setAction(NfcAdapter.ACTION_TAG_DISCOVERED)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
                }
            } catch (e: Throwable) {
                errCatchTagDebug().error(e)
            }
        }

        val debugAsk = false
        if (null == activity || !debugAsk) onYes() else
            try {
                ask(activity, null, ::onYes)
            } catch (e: Throwable) {
                errCatchTagDebug().error(e)
            }
    }
}

open class NfcTag(private val packageName: String, protected val onError: ((String, Throwable?) -> Unit)? = null,
                  private val tag: Tag?, private val ndefMessages: List<NdefMessage>? = null,
                  protected val dump: MutableList<String>? = null, private val debugResponse: TagType? = null) {
    //Mifare Ultralight - https://www.nxp.com/docs/en/data-sheet/MF0ICU1.pdf
    //Mifare Ultralight C - https://www.nxp.com/docs/en/data-sheet/MF0ICU2.pdf
    //Mifare Ultralight EV1 - https://www.nxp.com/docs/en/data-sheet/MF0ULX1.pdf
    //NTag213, NTag215, NTag216 - https://www.nxp.com/docs/en/data-sheet/NTAG213_215_216.pdf
    //MIFARE Classic EV1 1K (s50) - https://www.nxp.com/docs/en/data-sheet/MF1S50YYX_V1.pdf
    //MIFARE Classic EV1 4K (s70) - https://www.nxp.com/docs/en/data-sheet/MF1S70YYX_V1.pdf
    //MIFARE DESFire Light - https://www.nxp.com/docs/en/data-sheet/MF2DLHX0.pdf
    //MIFARE DESFire EV1 - https://www.nxp.com/docs/en/data-sheet/MF3ICDX21_41_81_SDS.pdf
    //MIFARE DESFire EV1 256B - https://www.nxp.com/docs/en/data-sheet/MF3ICDQ1_MF3ICDHQ1_SDS.pdf
    //MIFARE DESFire EV2 - https://www.nxp.com/docs/en/data-sheet/MF3DX2_MF3DHX2_SDS.pdf
    //MIFARE DESFire EV3 - https://www.nxp.com/docs/en/data-sheet/MF3DHx3_SDS.pdf
    //MIFARE type identification procedure - https://www.nxp.com/docs/en/application-note/AN10833.pdf
    //MIFARE ISO/IEC 14443 PICC selection - https://www.nxp.com/docs/en/application-note/AN10834.pdf

    companion object {
        internal val TAG = NfcTag::class.java.name

        fun hexString(arr: ByteArray?): String? = if (null == arr || arr.isEmpty()) null else
            arr.joinToString("") { String.format("%02X", it) } // Or use ByteArray.toHexString()

        fun toByteArr(hexString: String?): ByteArray? = if (hexString.isNullOrEmpty()) null else
            hexString.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    // localize
    //private fun errorInvalidPageNumber() = NfcErr("Invalid page number", TAG, onError)
    private fun errThrowTranceiveUnsupportedTech() = NfcErr("This tag is not supported", TAG, onError, "Unsupported tech for tranceive")
    private fun errThrowTranceiveTagLost1() = NfcErr("Tag is disconnected (?)", TAG, onError, "Transceive result is null")
    private fun errThrowTranceiveTagLost2() = NfcErr("Tag is disconnected (??)", TAG, onError, "Transceive result is empty")
    private fun errThrowTranceiveNak(log: String) = NfcErr("Operation failed", TAG, onError, log)
    private fun errThrowInvalidData() = NfcErr("Invalid data", TAG, onError, "Invalid data size")
    private fun errThrowAuthenticationInvalidKey() = NfcErr("Invalid data", TAG, onError, "Invalid key size")
    private fun errThrowAuthenticationFailed(log: String) = NfcErr("Authentication failed", TAG, onError, log)
    private fun errThrowAuthenticationResponse(log: String) = NfcErr("Authentication failed", TAG, onError, log)
    private fun errThrowWriteUnsupportedTag(log: String) = NfcErr("This tag is not supported", TAG, onError, log)
    private fun errThrowWriteInvalidData() = NfcErr("Invalid data", TAG, onError, "Invalid data size")
    private fun errThrowWriteInvalidConfig() = NfcErr("Invalid data", TAG, onError, "Invalid config data")
    private fun errThrowWriteInvalidPAck() = NfcErr("Invalid data", TAG, onError, "Invalid PAck size")
    private fun errorWritePass(log: String) = NfcErr("Write password failed", TAG, onError, log)
    private fun errorWriteProtect(log: String) = NfcErr("Read-protect data failed", TAG, onError, log)

    enum class TagType(val version: List<Byte>) {
        NTag213   (listOf(0, 0x4, 0x4, 0x2, 0x1, 0, 0x0F, 0x3)),    // 0004040201000F03: NTag213
        NTag215   (listOf(0, 0x4, 0x4, 0x2, 0x1, 0, 0x11, 0x3)),    // 0004040201001103: NTag215
        NTag216   (listOf(0, 0x4, 0x4, 0x2, 0x1, 0, 0x13, 0x3)),    // 0004040201001303: NTag216
        MF0UL11   (listOf(0, 0x4, 0x3, 0x1, 0x1, 0, 0x0b, 0x3)),    // 0004030101000B03: MIFARE Ultralight EV1 MF0UL11
        MF0ULH11  (listOf(0, 0x4, 0x3, 0x2, 0x1, 0, 0x0b, 0x3)),    // 0004030201000B03: MIFARE Ultralight EV1 MF0ULH11
        MF0UL21   (listOf(0, 0x4, 0x3, 0x1, 0x1, 0, 0x0E, 0x3)),    // 0004030101000E03: MIFARE Ultralight EV1 MF0UL21
        MF0ULH21  (listOf(0, 0x4, 0x3, 0x2, 0x1, 0, 0x0E, 0x3)),    // 0004030201000E03: MIFARE Ultralight EV1 MF0ULH21
        //MF2DL1000 (listOf(0x4, 0x8, 0x01, 0x30, 0, 0x13, 0x91.toByte(), 0xAF.toByte())),            // 04080130001391AF: MIFARE DESFire Light
        //MF2DLH1000(listOf(0x4, 0x8, 0x02, 0x30, 0, 0x13, 0x91.toByte(), 0xAF.toByte())),            // 04080230001391AF: MIFARE DESFire Light
        //MF2DL1001 (listOf(0x4, 0x8, 0x81.toByte(), 0x30, 0, 0x13, 0x91.toByte(), 0xAF.toByte())),   // 04088130001391AF: MIFARE DESFire Light
        //MF2DLH1001(listOf(0x4, 0x8, 0x82.toByte(), 0x30, 0, 0x13, 0x91.toByte(), 0xAF.toByte())),   // 04088230001391AF: MIFARE DESFire Light
    }

    @Suppress("SpellCheckingInspection")
    enum class AuthenticationDefault(val bytes: ByteArray) {
        NTagPass            (ByteArray(4) { 0xFF.toByte() }),
        NTagPAck            (ByteArray(2) { 0 }),
        //MifareUltralightC   (byteArrayOf(0x42, 0x52, 0x45, 0x41, 0x4b, 0x4D, 0x45, 0x49, 0x46, 0x59, 0x4F, 0x55, 0x43, 0x41, 0x4E, 0x21)),
        MifareUltralightC   ("BREAKMEIFYOUCAN!".map { it.code.toByte() }.toByteArray()),
        //MifareClassic_4k    (ByteArray(5) { 0xFF.toByte() }),
        //MifareDESFireEV1_4k (ByteArray(8) { 0 }),
    }

    @Suppress("SpellCheckingInspection")
    class Tech(
        val isoDep: IsoDep? = null,
        val mifareClassic: MifareClassic? = null,
        val mifareUltralight: MifareUltralight? = null,
        val ndef: Ndef? = null,
        val ndefFormatable: NdefFormatable? = null,
        val nfcA: NfcA? = null,
        val nfcB: NfcB? = null,
        val nfcBarcode: NfcBarcode? = null,
        val nfcF: NfcF? = null,
        val nfcV: NfcV? = null,
    )

    //@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    protected val tech = if (null == tag) Tech() else
        Tech(isoDep = IsoDep.get(tag), mifareClassic = MifareClassic.get(tag), mifareUltralight = MifareUltralight.get(tag),
            ndef = Ndef.get(tag), ndefFormatable = NdefFormatable.get(tag), nfcA = NfcA.get(tag), nfcB = NfcB.get(tag),
            nfcBarcode = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) null else
                NfcBarcode.get(tag), nfcF = NfcF.get(tag), nfcV = NfcV.get(tag))

    protected val tagId = // 'Anti-cloning support by unique 7-byte serial number for each device'
        if (null == debugResponse) tag?.id
        else byteArrayOf(0x4, 0x2F, 0xD8.toByte(), 0xD2.toByte(), 0x28, 0x67, 0x81.toByte())

    @Throws(NfcErr::class, IOException::class)
    protected fun <T> connect(tagTech: TagTechnology, isNeeded: Boolean = true, call: () -> T): T {
        val needConnect = isNeeded && !tagTech.isConnected
        if (needConnect) tagTech.connect()
        try {
            return call()
        } finally {
            if (needConnect) tagTech.close()
        }
    }

    //===

    @Throws(NfcErr::class, IOException::class)
    private fun transceive(tagTech: TagTechnology, data: ByteArray?): ByteArray {
        val response = when (tagTech) { // Exception: NAK or timeout (tag was lost)
            is MifareUltralight -> tagTech.transceive(data)
            is NfcA -> tagTech.transceive(data)
            else -> throw errThrowTranceiveUnsupportedTech()
        }
        Log.d(TAG, "NFC transceive: After") //todo-op??? test read protect, write pass, key authentication
        val responseAck = 0xA.toByte()
        if (response == null) throw errThrowTranceiveTagLost1() // Err: NAK or timeout (tag was lost)
        if (response.isEmpty()) throw errThrowTranceiveTagLost2() //? Err: NAK or timeout (tag was lost)
        if ((response.size == 1) && ((response[0] and responseAck) != responseAck))
            throw errThrowTranceiveNak("Transceive result NAK 0x${hexString(response)}")
        return response // Success: response ACK or response data
    }

    private val tagError: Boolean
    protected val tagVersion: List<Byte>
    private val tagType: TagType?
    init {
        var error = false
        var version: List<Byte>? = if (null == debugResponse) null else TagType.NTag213.version
        try {
            tech.mifareUltralight?.also {
                connect(it) {
                    try {
                        version = transceive(it, byteArrayOf(0x60)).toList() // 0x60 = GET_VERSION
                    } catch (e: Throwable) {
                        error = true
                        NfcErr("Init version", TAG).err(e)
                    }
                }
            }
            if (null == tech.mifareUltralight || null == version) tech.nfcA?.also {
                connect(it) {
                    try {
                        version = transceive(it, byteArrayOf(0x60)).toList() // 0x60 = GET_VERSION
                    } catch (e: Throwable) {
                        NfcErr("Init version NfcA", TAG).err(e)
                    }
                }
            }
        } catch (e: Throwable) {
            error = true
            NfcErr("Init connect", TAG).err(e)
        }
        tagError = error
        tagVersion = version ?: emptyList()

        tagType = TagType.values().find {
            it.version == version
            //} ?: when (tagVersion.getOrElse(2) { 0 }.toUByte() and 0xF0 ) {
            //    0x01 -> TagType.x // MIFARE DESFire
            //    0x02 -> TagType.x // MIFARE Plus
            //    0x03 -> TagType.x // MIFARE Ultralight
            //    0x04 -> TagType.x // NTag
            //    0x07 -> TagType.x // NTag I2C
            //    0x08 -> TagType.x // MIFARE DESFire Light
            //    else -> null
            //} ?: run {
            //    val reqA = tech.nfcA?.atqa
            //    when {
            //        listOf(0, 0x44) == reqA?.toList() -> TagType.x // MIFARE Ultralight C
            //        listOf(0, 0x44) == reqA?.toList() -> TagType.x // MIFARE Plus 2K, SE(1K) (7 Byte UID)
            //        listOf(0, 0x42) == reqA?.toList() -> TagType.x // MIFARE Plus 4K (7 Byte UID)
            //        listOf(0, 0x04) == reqA?.toList() -> TagType.x // MIFARE Plus 2K, SE(1K) (4 Byte Non-UID)
            //        listOf(0, 0x02) == reqA?.toList() -> TagType.x // MIFARE Plus 4K (4 Byte Non-UID)
            //        listOf(0, 0x04) == reqA?.let { arr -> if (arr.size < 2) null else listOf(arr[0], arr[1] and 0x0F) } -> TagType.x // MIFARE Classic EV1 1K
            //        listOf(0, 0x04) == reqA?.let { arr -> if (arr.size < 2) null else listOf(arr[0], arr[1] and 0x0F) } -> TagType.x // MIFARE Classic EV1 4K
            //        else -> null
            //    }
        } ?: debugResponse

        //if (null == dump) NfcErr("NFC: tag error $tagError; type ${tagType}; version $tagVersion", TAG).log()
        dump()
    }

    //@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun dump() {
        if (null == dump) return
        if (dump.isNotEmpty()) {
            dump.add(0, "Intent:")
            dump.add("")
        }

        fun dumpToList(list: MutableList<String>, prefix: String, ndefMessage: NdefMessage?) {
            ndefMessage?.records?.forEachIndexed { recIdx, rec ->
                list.add("$prefix$recIdx: ${rec.tnf}; ${hexString(rec.type)}; ${hexString(rec.id)}; ${hexString(rec.payload)}")
            } ?: list.add("$prefix$ndefMessage")

        }

        fun addToDump(message: String, list: List<String>) {
            if (list.isNotEmpty()) {
                dump.add(message)
                dump.addAll(list)
                dump.add("")
            }
        }

        addToDump("Intent NdefMessages:", mutableListOf<String>().also { list ->
            ndefMessages?.forEachIndexed { msgIdx, ndefMessage ->
                dumpToList(list, "$msgIdx, ", ndefMessage)
            }
        })

        addToDump("INFO:", mutableListOf<String>().also { list ->
            debugResponse?.let { list.add("debugResponse: $it") }
            list.add("id: ${hexString(tagId)}")
            list.add("techList: ${tag?.techList?.joinToString()}")
            tech.ndef?.also {
                list.add("")
                list.add("Ndef.type: ${it.type}" + when (it.type) {
                    Ndef.NFC_FORUM_TYPE_1 -> " / Innovision Topaz"
                    Ndef.NFC_FORUM_TYPE_2 -> " / Mifare Ultralight"
                    Ndef.NFC_FORUM_TYPE_3 -> " / Sony Felica"
                    Ndef.NFC_FORUM_TYPE_4 -> " / Mifare DESFire"
                    else -> ""
                })
                list.add("Ndef.maxSize: ${it.maxSize}")
                list.add("Ndef.isWritable: ${it.isWritable}")
                list.add("Ndef.canMakeReadOnly: ${it.canMakeReadOnly()}")
                dumpToList(list, "Ndef.cachedMessage: ", it.cachedNdefMessage)
            }
            tech.mifareUltralight?.also {
                list.add("")
                list.add("MifareUltralight.type: " + when (it.type) {
                    MifareUltralight.TYPE_ULTRALIGHT -> "ULTRALIGHT"
                    MifareUltralight.TYPE_ULTRALIGHT_C -> "ULTRALIGHT_C"
                    MifareUltralight.TYPE_UNKNOWN -> "UNKNOWN"
                    else -> "UL-${it.type}"
                })
                //list.add("MifareUltralight.maxTransceiveLength: ${it.maxTransceiveLength}")
                //list.add("MifareUltralight.timeout: ${it.timeout}")
            }
            tech.nfcA?.also {
                list.add("")
                list.add("NfcA.atqa: ${hexString(it.atqa)}")
                list.add("NfcA.sak: ${it.sak}")
                //list.add("NfcA.maxTransceiveLength: ${it.maxTransceiveLength}")
                //list.add("NfcA.timeout: ${it.timeout}")
            }
            tech.isoDep?.also {
                list.add("")
                list.add("IsoDep.hiLayerResponse: ${hexString(it.hiLayerResponse)}")
                list.add("IsoDep.historicalBytes: ${hexString(it.historicalBytes)}")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    list.add("IsoDep.isExtendedLengthApduSupported: ${it.isExtendedLengthApduSupported}")
                list.add("IsoDep.maxTransceiveLength: ${it.maxTransceiveLength}")
                list.add("IsoDep.timeout: ${it.timeout}")
            }
        })

        addToDump("DETAILS:", mutableListOf<String>().also { list ->
            list.add("tagError: $tagError")
            list.add("tagType: $tagType")
            list.add("tagVersion: ${hexString(tagVersion.toByteArray())}")
            try {
                tech.ndef?.also {
                    connect(it) {
                        try {
                            dumpToList(list, "Ndef.message: ", it.ndefMessage)
                        } catch (e: Throwable) {
                            list.add("ERROR: Dump Ndef.message: $e")
                        }
                    }
                }
                tech.mifareUltralight?.also {
                    connect(it) {
                        try {
                            val readablePages = 0..
                                    if (tagType == TagType.NTag213) 0x2C // last page (roll-over): 0x2C
                                    else if (tagType == TagType.NTag215) 0x84 // last page (roll-over): 0x86
                                    else if (tagType == TagType.NTag216) 0xE4 // last page (roll-over): 0xE6
                                    else if (listOf(TagType.MF0UL11, TagType.MF0ULH11).contains(tagType)) 0x10 // last page (roll-over): 0x13
                                    else if (listOf(TagType.MF0UL21, TagType.MF0ULH21).contains(tagType)) 0x28 // last page (roll-over): 0x28
                                    else if (it.type == MifareUltralight.TYPE_ULTRALIGHT) 0xC // last page: 0xF
                                    else if (it.type == MifareUltralight.TYPE_ULTRALIGHT_C) 0x28 // last page (roll-over): 0x2b
                                    else -1
                            list.add("MifareUltralight.readPages: $readablePages")
                            for (page in readablePages step 4) {
                                val pages: ByteArray? = it.readPages(page) // read 4 pages
                                list.add("${page}..${page + 3}: " + hexString(pages))
                            }
                        } catch (e: Throwable) {
                            list.add("ERROR: MifareUltralight.readPages: $e")
                        }
                    }
                    if (tech.mifareUltralight.type == MifareUltralight.TYPE_ULTRALIGHT_C)
                        connect(it) {
                            try {
                                //val key = Default.MifareUltralightC.bytes
                                val key = byteArrayOf(*AuthenticationDefault.MifareUltralightC.bytes.take(8).reversed().toByteArray(),
                                    *AuthenticationDefault.MifareUltralightC.bytes.takeLast(8).reversed().toByteArray())
                                it.timeout = 5000 //todo-op??? test read protect, write pass, key authentication
                                mifareUltralightTryAuthenticate(it, key) //todo-op!!! android.nfc.TagLostException: Tag was lost.
                                list.add("mifareUltralightAuthenticate: OK")
                            } catch (e: Throwable) {
                                list.add("ERROR: mifareUltralightAuthenticate: $e")
                            }
                        }
                }
            } catch (e: Throwable) {
                list.add("ERROR: Dump: $e")
            }
        })

        if (dump.isNotEmpty()) NfcErr("NFC: dump\n${dump.joinToString("\n")}", TAG).log()
    }

    //===

    //@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun ndefMsgRecord(data: ByteArray?): NdefRecord? {
        // Intent filter is needed in AndroidManifest.xml (for both formats?): <intent-filter><action android:name="android.nfc.action.NDEF_DISCOVERED" />...
        val useAppRecord = false
        return if (useAppRecord) NdefRecord.createApplicationRecord(packageName)
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            NdefRecord.createMime("application/$packageName", data)
        else null
    }

    protected fun ndefMsgRecordFilter(data: ByteArray? = null): List<NdefRecord?>? {
        val rec = ndefMsgRecord(data)
        return tech.ndef?.cachedNdefMessage?.records?.filter {
            rec?.tnf == it.tnf && rec.type?.toList() == it.type?.toList() && rec.id?.toList() == it.id?.toList()
                    && (null == rec.payload || rec.payload.isEmpty() || rec.payload?.toList() == it.payload?.toList())
        }
    }

    protected fun ndefMsgRecordFind(data: ByteArray? = null): NdefRecord? =
        ndefMsgRecordFilter(data)?.getOrNull(0)

    @Throws(FormatException::class, IOException::class)
    protected fun ndefMessage(records: List<NdefRecord>, data: ByteArray?): NdefMessage? {
        if (null != ndefMsgRecordFind(data)) return null // Ok: record exists
        val rec = ndefMsgRecordFind()
        return NdefMessage(records.map { if (null == rec || it != rec) it else ndefMsgRecord(data) } // replace existing record
            .toMutableList().also { if (null == rec) it.add(ndefMsgRecord(data)) } // add new record
            .toTypedArray())
    }

    //===

    protected val mifareUltralightPageUserMin: Int =
        when (tagType) {
            TagType.NTag213 -> 6 // keep memory content at delivery? - 'data pages 04h and 05h of NTag21x are pre-programmed'
            TagType.NTag215 -> 6 // keep memory content at delivery? - 'data pages 04h and 05h of NTag21x are pre-programmed'
            TagType.NTag216 -> 6 // keep memory content at delivery? - 'data pages 04h and 05h of NTag21x are pre-programmed'
            else -> 4
        }
    protected val mifareUltralightPageUserMax: Int =
        if (tagType == TagType.NTag213) 0x27
        else if (tagType == TagType.NTag215) 0x81
        else if (tagType == TagType.NTag216) 0xE1
        else if (listOf(TagType.MF0UL11, TagType.MF0ULH11).contains(tagType)) 0xF
        else if (listOf(TagType.MF0UL21, TagType.MF0ULH21).contains(tagType)) 0x23
        else if (tech.mifareUltralight?.type == MifareUltralight.TYPE_ULTRALIGHT) 0xF
        else if (tech.mifareUltralight?.type == MifareUltralight.TYPE_ULTRALIGHT_C) 0x27
        else mifareUltralightPageUserMin - 1

    //@Throws(NfcErr::class)
    //private fun mifareUltraLightPageCheck(page: Int?) {
    //    if (null == page || page < mifareUltralightPageUserMin || page > mifareUltralightPageUserMax)
    //        throw errorInvalidPageNumber()
    //}

    @Throws(NfcErr::class, IOException::class)
    protected fun mifareUltralightFindPage(tagTech: MifareUltralight, data: List<Byte>, stop: List<Byte>? = null, onStop: ((Int) -> Int)? = null, onData: ((Int) -> Unit)? = null): Int? {
        if (data.size != 4) throw errThrowInvalidData()
        return connect(tagTech) {
            for (page in mifareUltralightPageUserMax downTo mifareUltralightPageUserMin step 4) {
                val bytes: ByteArray? = tagTech.readPages(page - 3) // read 4 pages
                val pages = bytes?.toList()
                for (relative in 3 downTo 0) {
                    if (page - 3 + relative < mifareUltralightPageUserMin) break
                    pages?.chunked(4)?.getOrNull(relative)?.let {
                        if (it == data) {
                            onData?.invoke(page - 3 + relative)
                            return@connect page - 3 + relative
                        } else if (it == stop) {
                            return@connect onStop?.invoke(page - 3 + relative)
                        }
                    }
                }
            }
            null
        }
    }

    @Throws(NfcErr::class, IOException::class)
    protected fun mifareUltralightWritePage(tagTech: MifareUltralight, page: Int, data: List<Byte>, useTransceive: Boolean = false, useWriteCompatible: Boolean = false) {
        if (data.size != 4) throw errThrowWriteInvalidData()
        if (!useTransceive) tagTech.writePage(page, data.toByteArray())
        else if (useWriteCompatible)
            transceive(tagTech, byteArrayOf(0xA0.toByte(), page.toByte(), *ByteArray(12) { 0 }, data[0], data[1], data[2], data[3])) // 0xA0 = COMPATIBILITY_WRITE
        else transceive(tagTech, byteArrayOf(0xA2.toByte(), page.toByte(), data[0], data[1], data[2], data[3])) // 0xA2 = WRITE
    }

    @Throws(NfcErr::class, IOException::class,
        NoSuchAlgorithmException::class, NoSuchPaddingException::class,
        InvalidAlgorithmParameterException::class, InvalidKeyException::class, InvalidKeySpecException::class,
        BadPaddingException::class, IllegalBlockSizeException::class)
    private fun mifareUltralightTryAuthenticate(tagTech: TagTechnology, key: ByteArray) {
        // From: https://stackoverflow.com/questions/19438554/android-authenticating-with-nxp-mifare-ultralight-c
        fun rotateLeft(arr: ByteArray) = if (arr.size <= 1) arr else byteArrayOf(*arr.takeLast(arr.size - 1).toByteArray(), arr[0])
        fun check(res: ByteArray?, answer: Byte) = if (res?.size == 9 && res[0] == answer) res else
            throw errThrowAuthenticationResponse("Invalid response")
        fun performDes(opMode: Int, key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray {
            val desKeyFactory = SecretKeyFactory.getInstance("DESede")
            val desKey = desKeyFactory.generateSecret(DESedeKeySpec(byteArrayOf(*key, *key.take(8).toByteArray())))
            val des = Cipher.getInstance("DESede/CBC/NoPadding")
            des.init(opMode, desKey, IvParameterSpec(iv))
            return des.doFinal(data)
        }
        if (key.size != 16) throw errThrowAuthenticationInvalidKey()
        val desRndB = check(transceive(tagTech, byteArrayOf(0x1A)), 0xAF.toByte()) // 0x1A = AUTHENTICATE; 0xAF = Ok
        Log.d(TAG, "NFC mifareUltralightTryAuthenticate: After") //todo-op??? test read protect, write pass, key authentication
        val rndB = performDes(Cipher.DECRYPT_MODE, key, ByteArray(8) { 0 }, desRndB.takeLast(8).toByteArray())
        val rndA = ByteArray(8).also { SecureRandom().nextBytes(it) }
        val desRndARndBRot = performDes(Cipher.ENCRYPT_MODE, key, desRndB, byteArrayOf(*rndA, *rotateLeft(rndB)))
        val desRndARot = check(transceive(tagTech, byteArrayOf(0xAF.toByte(), *desRndARndBRot)), 0) // 0x1A = AUTHENTICATE; 0 = Ok
        val rndARot = performDes(Cipher.DECRYPT_MODE, key, desRndARndBRot.takeLast(8).toByteArray(), desRndARot.takeLast(8).toByteArray())
        if (rotateLeft(rndA).toList() != rndARot.toList()) throw errThrowAuthenticationFailed("Key authentication failed")
    }

    //===

    protected val nTagPassPage: Int? = when (tagType) {
        TagType.NTag213 -> 0x2b
        TagType.NTag215 -> 0x85
        TagType.NTag216 -> 0xE5
        TagType.MF0UL11 -> 0x12
        TagType.MF0ULH11 -> 0x12
        TagType.MF0UL21 -> 0x27
        TagType.MF0ULH21 -> 0x27
        else -> null
    }

    protected val nTagConfigPage = when (tagType) {
        TagType.NTag213 -> 0x29
        TagType.NTag215 -> 0x83
        TagType.NTag216 -> 0xE3
        TagType.MF0UL11 -> 0x10
        TagType.MF0ULH11 -> 0x10
        TagType.MF0UL21 -> 0x25
        TagType.MF0ULH21 -> 0x25
        else -> null
    }

    @Throws(NfcErr::class, IOException::class)
    protected fun nTagWritePassAndAck(tagTech: MifareUltralight, pagePwd: List<Byte>, pwdAck: List<Byte>) {
        if (null == nTagPassPage) throw errThrowWriteUnsupportedTag("Unsupported NTag type (Pwd/PAck)")
        if (pwdAck.size != 2) throw errThrowWriteInvalidPAck()
        try {
            connect(tagTech) {
                tagTech.timeout = 5000 //todo-op??? test read protect, write pass, key authentication
                //todo-op!!! variant 1: java.io.IOException: Transceive failed
                mifareUltralightWritePage(tagTech, nTagPassPage, pagePwd)
                mifareUltralightWritePage(tagTech, nTagPassPage + 1, pwdAck.toMutableList().also { it.addAll(listOf(0, 0)) })
                //todo-op!!! variant 2: java.io.IOException: Transceive failed
                //mifareUltralightWritePage(tagTech, nTagPassPage, pagePwd, true)
                //mifareUltralightWritePage(tagTech, nTagPassPage + 1, pwdAck.toMutableList().also { it.addAll(listOf(0, 0)) }, true)
            }
        } catch (e: Throwable) {
            throw errorWritePass(e.toString())
        }
    }

    @Throws(NfcErr::class, IOException::class)
    protected fun nTagWriteProtect(tagTech: MifareUltralight, page: Int?) {
        if (null == nTagConfigPage) throw errThrowWriteUnsupportedTag("Unsupported NTag type (config)")
        if (null != page) try {
            connect(tagTech) {
                val pagesConfig: ByteArray? = tagTech.readPages(nTagConfigPage) // read 4 pages
                val pageCfg0 = pagesConfig?.take(4)
                val pageCfg1 = pagesConfig?.slice(4..7)
                if (pageCfg0?.size != 4 || pageCfg1?.size != 4) throw errThrowWriteInvalidConfig()
                val pageCfg10 = pageCfg1[0] or 0x80.toByte() // protect read and write
                if (page < pageCfg0[3].toUByte().toInt() || pageCfg10 != pageCfg1[0])
                    connect(tagTech) {
                        tagTech.timeout = 5000 //todo-op??? test read protect, write pass, key authentication
                        if (page < pageCfg0[3].toUByte().toInt()) // protect the page and next pages
                            mifareUltralightWritePage(tagTech, nTagConfigPage, listOf(pageCfg0[0], pageCfg0[1], pageCfg0[2], page.toByte()))
                        if (pageCfg10 != pageCfg1[0])
                            mifareUltralightWritePage(tagTech, nTagConfigPage, listOf(pageCfg10, pageCfg1[1], pageCfg1[2], pageCfg1[3])) //todo-op!!! java.io.IOException: Transceive failed
                    }
            }
        } catch (e: Throwable) {
            throw errorWriteProtect(e.toString())
        }
    }

    @Throws(NfcErr::class, IOException::class)
    protected fun nTagTryAuthenticate(tagTech: MifareUltralight, pagePwd: List<Byte>, pwdAck: List<Byte>, orStop: Boolean) {
        connect(tagTech) {
            var res: ByteArray? = null; var def: ByteArray? = null
            try {
                res = transceive(tagTech, byteArrayOf(0x1b.toByte(), *pagePwd.toByteArray())) // 0x1b = PWD_AUTH
            } catch (e: Throwable) {
                //Log.d(TAG, "NFC error: nTagTryPassAuthenticate: $e")
                if (e is TagLostException) { tagTech.close(); tagTech.connect() } // reconnect is needed
                try {
                    def = transceive(tagTech, byteArrayOf(0x1b.toByte(), *AuthenticationDefault.NTagPass.bytes)) // 0x1b = PWD_AUTH
                } catch (e: Throwable) {
                    //Log.d(TAG, "NFC error: nTagTryPassAuthenticate (Default): $e")
                    if (orStop) throw errThrowAuthenticationFailed("Password authentication failed")
                }
            }
            if (res != null)
                if (res.toList() != pwdAck) throw errThrowAuthenticationResponse("Invalid PAck")
                else NfcErr("NFC: Password authentication OK", TAG).log()
            if (def != null)
                if (def.toList() != AuthenticationDefault.NTagPAck.bytes.toList()) throw errThrowAuthenticationResponse("Invalid PAck (Default)")
                else NfcErr("NFC: Password authentication OK (Default)", TAG).log()
        }
    }
}

class NfcTagUnlock(packageName: String, onError: ((String, Throwable?) -> Unit)? = null,
                   tag: Tag?, ndefMessages: List<NdefMessage>? = null,
                   dump: MutableList<String>? = null, debugResponse: TagType? = null) : NfcTag(packageName, onError, tag, ndefMessages, dump, debugResponse) {
    companion object {
        private const val UnlockUniqueSize = 2 // unique bytes/checksum size = 2 bytes

        fun unlockUnique(value: ByteArray): List<Byte>? = if (UnlockUniqueSize == 0) null else
            ByteArray(UnlockUniqueSize).also { res ->
                for (index in 0 until UnlockUniqueSize) res[index] = 0
                if (UnlockUniqueSize > 0) value.forEachIndexed { index, byte ->
                    val data = if (index == 0) 2 * byte.toUByte().toInt() else byte.toInt() // obfuscate/hide when value.size = 1
                    if (index == 0) // obfuscate/hide when value.size <= unlockUniqueSize
                        for (item in 0 until UnlockUniqueSize) res[item] = (res[item].toUByte().toInt() + data).toByte()
                    else (index % UnlockUniqueSize).let { item ->
                        res[item] = (res[item].toUByte().toInt() + data).toByte()
                    }
                }
            }.toList()
    }

    // localize
    private fun errThrowWriteReadOnly() = NfcErr("Can not make read-only", TAG, onError)
    private fun errThrowWriteOutOfSpace() = NfcErr("Out of space", TAG, onError, "Empty page not found")
    private fun errThrowWriteUnsupportedTech() = NfcErr("This tag is not supported", TAG, onError, "Unsupported tech for write")
    private fun errCatchWrite() = NfcErr("Write failed", TAG, onError)
    private fun errCatchWriteAsk() = NfcErr("Parameters failed", TAG, onError)
    private fun errCatchInfoAsk() = NfcErr("Info failed", TAG, onError)

    val unlockNfcTag =
        mutableListOf<Byte>().let {
            it.addAll(tagId?.toList().orEmpty()) // 'Anti-cloning support by unique 7-byte serial number for each device'
            it.addAll(tagVersion.toList())
            if (it.isEmpty()) null else it.toList()
        }
    val unlockCanWrite = (null != tech.ndef && tech.ndef.isWritable)
            || null != tech.mifareUltralight

    @Throws(NfcErr::class, IOException::class)
    fun unlockCheck(value: ByteArray): Boolean {
        val unique = unlockUnique(value)
        if (null != tech.ndef)
            if (unique == ndefMsgRecordFind()?.payload?.toList()) {
                NfcErr("NFC: Unlock Ndef", TAG).log()
                return true
            }
        if (null != tech.mifareUltralight) {
            val arr = unique.orEmpty().toTypedArray()
            val pass = listOf(*arr, *arr, *arr, *arr).take(4)
            val pAck = listOf(*arr, *arr).take(2)
            val data = listOf(*arr, *arr, *arr, *arr).take(4)

            if (null != nTagPassPage) nTagTryAuthenticate(tech.mifareUltralight, pass, pAck, false)
            //else if (tech.mifareUltralight.type == MifareUltralight.TYPE_ULTRALIGHT_C) mifareUltralightTryAuthenticate(it, key)

            if (null != mifareUltralightFindPage(tech.mifareUltralight, data, listOf(0, 0, 0, 0))) {
                NfcErr("NFC: Unlock MifareUltralight", TAG).log()
                return true
            }
        }
        return false
    }

    @Throws(NfcErr::class, IOException::class)
    fun unlockWrite(value: ByteArray,
                          ndefClearMessage: Boolean, ndefClearRecord: Boolean, ndefIgnore: Boolean,
                          ndefMakeReadOnly: Boolean, ndefFormat: Boolean, miUlClearPages: Boolean, miUlClearLast: Boolean,
                          nTagClearPass: Boolean, nTagClearProtect: Boolean, nTagPass: Boolean,
                          nTagProtect: Boolean, nTagProtectConfig: Boolean, nTagProtectPass: Boolean): Boolean {
        try {
            fun ndefClearRecord(tagTech: Ndef) {
                val recs = ndefMsgRecordFilter()
                if (!recs.isNullOrEmpty()) {
                    val msg = NdefMessage(tagTech.cachedNdefMessage?.records.orEmpty()
                        .filter { !recs.contains(it) }.toMutableList().also {
                            if (it.isEmpty()) it.add(NdefRecord(NdefRecord.TNF_EMPTY, null, null, null))
                        }.toTypedArray())
                    connect(tagTech) {
                        tagTech.writeNdefMessage(msg)
                    }
                }
            }

            if (null != tech.ndef)
                if (ndefClearMessage) {
                    connect(tech.ndef) {
                        tech.ndef.writeNdefMessage(NdefMessage(NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)))
                    }
                    return false // undo
                } else if (ndefClearRecord) {
                    ndefClearRecord(tech.ndef)
                    return false // undo
                }

            val unique = unlockUnique(value)
            if (null != tech.ndef && !ndefIgnore) {
                ndefMessage(tech.ndef.cachedNdefMessage?.records.orEmpty().toList(), unique?.toByteArray())?.let {
                    connect(tech.ndef) {
                        tech.ndef.writeNdefMessage(it)
                    }
                }
                if (ndefMakeReadOnly)
                    if (!tech.ndef.canMakeReadOnly()) throw errThrowWriteReadOnly() else
                        connect(tech.ndef) {
                            tech.ndef.makeReadOnly()
                        }
            } else if (null != tech.ndefFormatable && ndefFormat) {
                ndefMessage(listOf(), unique?.toByteArray())?.let {
                    connect(tech.ndefFormatable) {
                        if (ndefMakeReadOnly) tech.ndefFormatable.formatReadOnly(it)
                        else tech.ndefFormatable.format(it)
                    }
                }
            } else if (null != tech.mifareUltralight) {
                //if (null != tech.ndef && ndefIgnore) ndefClearRecord(tech.ndef) // auto vs help + manual?

                val arr = unique.orEmpty().toTypedArray()
                val pass = listOf(*arr, *arr, *arr, *arr).take(4)
                val pAck = listOf(*arr, *arr).take(2)
                val data = listOf(*arr, *arr, *arr, *arr).take(4)

                if (null != nTagPassPage) nTagTryAuthenticate(tech.mifareUltralight, pass, pAck, false)
                //else if (tech.mifareUltralight.type == MifareUltralight.TYPE_ULTRALIGHT_C) mifareUltralightTryAuthenticate(it, key)

                if (miUlClearPages || miUlClearLast) connect(tech.mifareUltralight) {
                    for (page in (if (miUlClearPages) mifareUltralightPageUserMin else mifareUltralightPageUserMax)..mifareUltralightPageUserMax)
                        mifareUltralightWritePage(tech.mifareUltralight, page, listOf(0, 0, 0, 0))
                }
                if (null != nTagPassPage) {
                    if (nTagClearProtect) nTagWriteProtect(tech.mifareUltralight, 0xFF)
                    if (nTagClearPass) nTagWritePassAndAck(tech.mifareUltralight, AuthenticationDefault.NTagPass.bytes.toList(), AuthenticationDefault.NTagPAck.bytes.toList())
                }
                if (miUlClearPages || miUlClearLast || nTagClearProtect || nTagClearPass) return false // undo

                val page = mifareUltralightFindPage(tech.mifareUltralight, listOf(0, 0, 0, 0), data, onStop = { it }) {
                    //mifareUltraLightPageCheck(it)
                    connect(tech.mifareUltralight) {
                        mifareUltralightWritePage(tech.mifareUltralight, it, data) // write to last empty page
                    }
                } ?: throw errThrowWriteOutOfSpace()

                if (null != nTagPassPage) {
                    if (nTagProtect) {
                        //mifareUltraLightPageCheck(page)
                        nTagWriteProtect(tech.mifareUltralight, page)
                    } else if (nTagProtectConfig) nTagWriteProtect(tech.mifareUltralight, nTagConfigPage)
                    else if (nTagProtectPass) nTagWriteProtect(tech.mifareUltralight, nTagPassPage)
                    if (nTagPass) nTagWritePassAndAck(tech.mifareUltralight, pass, pAck)
                }
            } else
                throw errThrowWriteUnsupportedTech()
            return true
        } catch (e: Throwable) {
            errCatchWrite().error(e)
            return false
        }
    }

    fun unlockWriteAsk(context: Context, overwrite: Boolean, onNo: () -> Unit,
                       onYes: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean,
                               Boolean, Boolean, Boolean, Boolean) -> Unit) {
        try {
            fun switch(text: String, checked: Boolean) = SwitchCompat(context).also {
                it.setPadding(10, 0, 0, 75)
                it.text = text
                it.isChecked = checked
            }

            //val editTest = android.widget.EditText(that.context).also { it.setText("message") }
            val nfcNoWrite          = if (!unlockCanWrite)                                                             null else switch("nfcNoWrite", !unlockCanWrite)
            val ndefClearMessage    = if (!unlockCanWrite || null == tech.ndef)                                        null else switch("ndefClearMessage", false)
            val ndefClearRecord     = if (!unlockCanWrite || null == tech.ndef)                                        null else switch("ndefClearRecord", false)
            val ndefIgnore          = if (!unlockCanWrite || null == tech.ndef)                                        null else switch("ndefIgnore", null == ndefMsgRecordFind())
            val ndefMakeReadOnly    = if (!unlockCanWrite || null == tech.ndef && null == tech.ndefFormatable)         null else switch("ndefMakeReadOnly", false)
            val ndefFormat          = if (!unlockCanWrite || null == tech.ndefFormatable)                              null else switch("ndefFormat", false)
            val miUlClearPages      = if (!unlockCanWrite || null == tech.mifareUltralight)                            null else switch("miUlClearPages", false)
            val miUlClearLast       = if (!unlockCanWrite || null == tech.mifareUltralight)                            null else switch("miUlClearLast", false)
            val nTagClearPass       = if (!unlockCanWrite || null == tech.mifareUltralight || null == nTagPassPage)    null else switch("nTagClearPass", false)
            val nTagClearProtect    = if (!unlockCanWrite || null == tech.mifareUltralight || null == nTagPassPage)    null else switch("nTagClearProtect", false)
            val nTagPass            = if (!unlockCanWrite || null == tech.mifareUltralight || null == nTagPassPage)    null else switch("nTagPass", false)
            val nTagProtect         = if (!unlockCanWrite || null == tech.mifareUltralight || null == nTagPassPage)    null else switch("nTagProtect", false)
            val nTagProtectConfig   = if (!unlockCanWrite || null == tech.mifareUltralight || null == nTagPassPage)    null else switch("nTagProtectConfig", false)
            val nTagProtectPass     = if (!unlockCanWrite || null == tech.mifareUltralight || null == nTagPassPage)    null else switch("nTagProtectPass", false)

            fun onYes() {
                onYes(nfcNoWrite?.isChecked ?: false,
                    ndefClearMessage?.isChecked ?: false, ndefClearRecord?.isChecked ?: false, ndefIgnore?.isChecked ?: false,
                    ndefMakeReadOnly?.isChecked ?: false, ndefFormat?.isChecked ?: false, miUlClearPages?.isChecked ?: false, miUlClearLast?.isChecked ?: false,
                    nTagClearPass?.isChecked ?: false, nTagClearProtect?.isChecked ?: false, nTagPass?.isChecked ?: false,
                    nTagProtect?.isChecked ?: false, nTagProtectConfig?.isChecked ?: false, nTagProtectPass?.isChecked ?: false)
            }

            if (null == dump && !unlockCanWrite) {
                onYes()
                return
            }
            AlertDialog.Builder(context)
                .setNegativeButton(context.getString(android.R.string.cancel)) { dialog, _ -> dialog.cancel() }
                .setOnCancelListener { onNo() }
                .setPositiveButton(if (overwrite) "Overwrite" else context.getString(android.R.string.ok)) { _, _ -> onYes() }
                .also {
                    if (!unlockCanWrite) it.setTitle("WRITE NOT SUPPORTED")
                    //dump?.let { dump -> it.setMessage(dump.joinToString("\n")) }
                }.setView(ScrollView(context).also { scroll ->
                    scroll.addView(LinearLayout(context).also { layout ->
                        layout.orientation = LinearLayout.VERTICAL
                        layout.setPadding(60, 60, 60, 0)
                        //if (!unlockCanWrite) layout.addView(TextView(context).also { it.text = "WRITE NOT SUPPORTED" })
                        layout.addView(TextView(context).also {
                            it.text = "NOTES: Current status\n" +
                                    "1. Not tested because of 'java.io.IOException: Transceive failed':\n" +
                                    "'nTagPass' - write password to NTag or MIFARE Ultralight EV1),\n" +
                                    "'nTagProtect*' - write config to protect reading the pages\n" +
                                    "2. Not tested and not complete because of 'android.nfc.TagLostException: Tag was lost':\n" +
                                    "authentication for MifareUltralight C\n"
                        })
                        dump?.let { dump ->
                            layout.addView(TextView(context).also { it.text = dump.joinToString("\n") })
                        }
                        if (unlockCanWrite) listOfNotNull(ndefClearMessage, ndefClearRecord, ndefIgnore,
                            ndefMakeReadOnly, ndefFormat, miUlClearPages, miUlClearLast,
                            nTagClearPass, nTagClearProtect, nTagPass,
                            nTagProtect, nTagProtectConfig, nTagProtectPass,
                        ).forEach { layout.addView(it) }
                    })
                }).create().show()
        } catch (e: Throwable) {
            errCatchWriteAsk().error(e)
        }
    }

    fun unlockInfoAsk(context: Context, onNo: () -> Unit, onYes: () -> Unit) =
        try {
            if (dump.isNullOrEmpty())
                try {
                    onYes()
                } catch (e: Throwable) {
                    errCatchInfoAsk().errorCb(e)
                }
            else AlertDialog.Builder(context)
                .setNegativeButton(context.getString(android.R.string.cancel)) { dialog, _ -> dialog.cancel() }
                .setOnCancelListener {
                    try {
                        onNo()
                    } catch (e: Throwable) {
                        errCatchInfoAsk().errorCb(e)
                    }
                }.setPositiveButton(context.getString(android.R.string.ok)) { _, _ ->
                    try {
                        onYes()
                    } catch (e: Throwable) {
                        errCatchInfoAsk().errorCb(e)
                    }
                }.also {
                    if (!unlockCanWrite) it.setTitle("WRITE NOT SUPPORTED")
                    //it.setMessage(dump.joinToString("\n"))
                }.setView(ScrollView(context).also { scroll ->
                    scroll.addView(LinearLayout(context).also { layout ->
                        layout.orientation = LinearLayout.VERTICAL
                        layout.setPadding(60, 60, 60, 0)
                        //if (!unlockCanWrite) layout.addView(TextView(context).also { it.text = "WRITE NOT SUPPORTED" })
                        layout.addView(TextView(context).also { it.text = dump.joinToString("\n") })
                    })
                }).create().show()
        } catch (e: Throwable) {
            errCatchInfoAsk().error(e)
        }
}

open class BaseErr(message: String?, private val tag: String,
                   private val onError: ((String, Throwable?) -> Unit)?, val log: String = "") : Exception(message) {
    open val prefixErr: String = ""

    private fun msg(prefix: String) = "$prefix$message ${if (log.isEmpty()) "" else "; "}$log"
    fun log() = Log.d(tag, msg(""))
    fun err(e: Throwable, isCallback: Boolean = false) =
        Log.d(tag, "%s%s: %s".format(msg(prefixErr), if (!isCallback) "" else " (callback)", e))
    fun errorCb(e: Throwable) = error(e, true)

    fun error(e: Throwable, isCallback: Boolean = false) {
        err(e, isCallback)
        onError?.invoke("$prefixErr$message" + if (e !is NfcErr || e.message.isNullOrBlank()) "" else "; ${e.message}",
            if (isCallback) e else null)
    }
}

class NfcErr(message: String?, tag: String, onError: ((String, Throwable?) -> Unit)? = null, log: String = "") : BaseErr(message, tag, onError, log) {
    override val prefixErr = "NFC error: " // localize
}
