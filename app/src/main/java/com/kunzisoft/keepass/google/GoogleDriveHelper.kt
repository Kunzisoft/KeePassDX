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

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes

class GoogleDriveHelper(private val context: Context) {

    private val googleSignInClient: GoogleSignInClient

    init {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, signInOptions)
    }

    fun signIn(): Intent {
        return googleSignInClient.signInIntent
    }

    fun handleSignInResult(data: Intent?, callback: (GoogleSignInAccount?) -> Unit) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
            .addOnSuccessListener { googleAccount ->
                callback(googleAccount)
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    fun getDriveService(googleSignInAccount: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            setOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = googleSignInAccount.account
        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential
        ).setApplicationName("KeepassDX").build()
    }

    fun uploadFile(driveService: Drive, folderId: String, fileName: String, fileContent: java.io.File, callback: (String?) -> Unit) {
        val fileMetadata = com.google.api.services.drive.model.File()
        fileMetadata.name = fileName
        fileMetadata.parents = listOf(folderId)

        val mediaContent = com.google.api.client.http.FileContent("application/octet-stream", fileContent)

        Thread {
            try {
                val file = driveService.files().create(fileMetadata, mediaContent).execute()
                callback(file.id)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }.start()
    }

    fun listFiles(driveService: Drive, folderId: String, callback: (List<com.google.api.services.drive.model.File>?) -> Unit) {
        Thread {
            try {
                val result = driveService.files().list()
                    .setQ("'$folderId' in parents and trashed = false")
                    .setFields("files(id, name)")
                    .execute()
                callback(result.files)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }.start()
    }

    fun downloadFile(driveService: Drive, fileId: String, callback: (java.io.ByteArrayOutputStream?) -> Unit) {
        Thread {
            try {
                val outputStream = java.io.ByteArrayOutputStream()
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                callback(outputStream)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }.start()
    }
}
