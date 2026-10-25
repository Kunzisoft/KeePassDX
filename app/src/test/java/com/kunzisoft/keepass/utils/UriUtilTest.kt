package com.kunzisoft.keepass.utils

import android.net.Uri
import android.provider.DocumentsContract
import com.kunzisoft.keepass.utils.UriUtil.canBeAccessedFromFolder
import com.kunzisoft.keepass.utils.UriUtil.getDocumentInTree
import com.kunzisoft.keepass.utils.UriUtil.getParentDocumentId
import com.kunzisoft.keepass.utils.UriUtil.getParentFolder
import com.kunzisoft.keepass.utils.UriUtil.getTreeOfDocument
import com.kunzisoft.keepass.utils.UriUtil.isDocumentIdInFolder
import com.kunzisoft.keepass.utils.UriUtil.isDocumentInTree
import com.kunzisoft.keepass.utils.UriUtil.isFolderIdBlockedFromTree
import com.kunzisoft.keepass.utils.UriUtil.isTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UriUtilTest {

    private val authority = "com.android.externalstorage.documents"

    // As returned by ACTION_OPEN_DOCUMENT
    private val file = Uri.parse(
        "content://$authority/document/primary%3Async%2FKeePass%2Fdatabase.kdbx")
    // As returned by ACTION_OPEN_DOCUMENT_TREE
    private val folder = Uri.parse("content://$authority/tree/primary%3Async%2FKeePass")

    @Test
    fun testGetParentDocumentId() {
        assertEquals("primary:sync/KeePass", getParentDocumentId("primary:sync/KeePass/database.kdbx"))
        assertEquals("primary:", getParentDocumentId("primary:database.kdbx"))
        assertEquals("1234-ABCD:a", getParentDocumentId("1234-ABCD:a/b"))
        assertNull(getParentDocumentId("primary:"))
        assertNull(getParentDocumentId("no-colon"))
    }

    @Test
    fun testIsDocumentIdInFolder() {
        assertTrue(isDocumentIdInFolder("primary:sync", "primary:sync/database.kdbx"))
        assertTrue(isDocumentIdInFolder("primary:sync", "primary:sync/a/database.kdbx"))
        assertTrue(isDocumentIdInFolder("primary:", "primary:database.kdbx"))
        assertFalse(isDocumentIdInFolder("primary:sync", "primary:sync"))
        assertFalse(isDocumentIdInFolder("primary:sync", "primary:syncthing/database.kdbx"))
        assertFalse(isDocumentIdInFolder("primary:sync", "1234-ABCD:sync/database.kdbx"))
    }

    @Test
    fun testIsFolderIdBlockedFromTree() {
        assertTrue(isFolderIdBlockedFromTree("primary:", 30))
        assertTrue(isFolderIdBlockedFromTree("1234-ABCD:", 30))
        assertTrue(isFolderIdBlockedFromTree("primary:Download", 30))
        assertTrue(isFolderIdBlockedFromTree("primary:Android/data", 30))
        assertTrue(isFolderIdBlockedFromTree("primary:Android/obb/app", 30))
        assertFalse(isFolderIdBlockedFromTree("primary:Download/KeePass", 30))
        assertFalse(isFolderIdBlockedFromTree("primary:sync/KeePass", 30))
        assertFalse(isFolderIdBlockedFromTree("primary:Android/database", 30))
        // No restriction before Android 11
        assertFalse(isFolderIdBlockedFromTree("primary:", 29))
        assertFalse(isFolderIdBlockedFromTree("primary:Download", 29))
    }

    @Test
    fun testFileCanBeAccessedFromFolder() {
        assertTrue(file.canBeAccessedFromFolder())
        assertFalse(file.isTree())
        assertFalse(file.isDocumentInTree())
        assertNull(file.getTreeOfDocument())
        assertEquals(
            DocumentsContract.buildDocumentUri(authority, "primary:sync/KeePass"),
            file.getParentFolder()
        )
        // Opaque document IDs of other providers
        assertFalse(Uri.parse("content://com.android.providers.media.documents/document/document%3A1000")
            .canBeAccessedFromFolder())
        assertFalse(Uri.parse("file:///storage/emulated/0/database.kdbx").canBeAccessedFromFolder())
        // A folder that cannot be selected (sdk 34)
        assertFalse(Uri.parse("content://$authority/document/primary%3ADownload%2Fdatabase.kdbx")
            .canBeAccessedFromFolder())
        assertFalse(Uri.parse("content://$authority/document/primary%3Adatabase.kdbx")
            .canBeAccessedFromFolder())
        assertTrue(Uri.parse("content://$authority/document/primary%3ADownload%2FKeePass%2Fdatabase.kdbx")
            .canBeAccessedFromFolder())
    }

    @Test
    fun testDocumentInTree() {
        val documentInTree = file.getDocumentInTree(folder)
        assertNotNull(documentInTree)
        documentInTree!!
        assertEquals(
            "content://$authority/tree/primary%3Async%2FKeePass" +
                    "/document/primary%3Async%2FKeePass%2Fdatabase.kdbx",
            documentInTree.toString()
        )
        assertEquals("primary:sync/KeePass/database.kdbx", DocumentsContract.getDocumentId(documentInTree))
        assertTrue(folder.isTree())
        assertFalse(documentInTree.isTree())
        assertTrue(documentInTree.isDocumentInTree())
        assertFalse(documentInTree.canBeAccessedFromFolder())
        // Same URI as the persisted tree permission
        assertEquals(folder, documentInTree.getTreeOfDocument())
        // A parent folder also contains the file
        assertNotNull(file.getDocumentInTree(Uri.parse("content://$authority/tree/primary%3Async")))
    }

    @Test
    fun testFolderWithoutDocument() {
        assertNull(file.getDocumentInTree(Uri.parse("content://$authority/tree/primary%3Async%2FOther")))
        assertNull(file.getDocumentInTree(Uri.parse("content://$authority/tree/primary%3Async%2FKeeP")))
        assertNull(file.getDocumentInTree(Uri.parse("content://com.example.documents/tree/primary%3Async")))
    }
}
