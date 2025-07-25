/*
 * Copyright 2023 The KeepassDX Team
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
 *
 */
package com.kunzisoft.keepass.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.services.drive.DriveScopes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class GoogleDriveHelperTest {

    @Test
    fun testGoogleDriveHelper() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val googleDriveHelper = GoogleDriveHelper(appContext)

        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_FILE))
            .build()
        val googleSignInClient = GoogleSignIn.getClient(appContext, signInOptions)

        val latch = CountDownLatch(1)
        var fileId: String? = null

        googleSignInClient.silentSignIn().addOnSuccessListener { account ->
            assertNotNull(account)
            val driveService = googleDriveHelper.getDriveService(account)
            val fileContent = "Hello, World!"
            val file = File(appContext.cacheDir, "test.txt")
            file.writeText(fileContent)

            googleDriveHelper.uploadFile(driveService, "root", "test.txt", file) { id ->
                fileId = id
                assertNotNull(fileId)

                googleDriveHelper.listFiles(driveService, "root") { files ->
                    assertNotNull(files)
                    val uploadedFile = files?.find { it.id == fileId }
                    assertNotNull(uploadedFile)
                    assertEquals("test.txt", uploadedFile?.name)

                    googleDriveHelper.downloadFile(driveService, fileId!!) { outputStream ->
                        assertNotNull(outputStream)
                        assertEquals(fileContent, outputStream?.toString())
                        latch.countDown()
                    }
                }
            }
        }

        latch.await()
    }
}
