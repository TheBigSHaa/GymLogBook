package com.ironlog.app.data.sync

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        /** Distinct from V1 (`ironlog_backup.json`); stored in Google Drive appDataFolder for this OAuth client. */
        private const val BACKUP_FILENAME = "ProYou_Backup_V2.json"
        private const val MIME_TYPE = "application/json"

        /** Upload limit (5 MB). Above this we refuse to encode the backup to avoid OOM on low-RAM devices. */
        private const val MAX_BACKUP_BYTES = 5L * 1024 * 1024
    }

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    sealed class Outcome<out T> {
        data class Ok<T>(val value: T) : Outcome<T>()
        data object NotSignedIn : Outcome<Nothing>()
        data object AuthRequired : Outcome<Nothing>()
        data object NotFound : Outcome<Nothing>()
        data class Failure(val error: Throwable) : Outcome<Nothing>()
    }

    private fun getDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("ProYou")
            .build()
    }

    suspend fun uploadBackup(data: BackupData): Outcome<Unit> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService() ?: return@withContext Outcome.NotSignedIn
            val json = gson.toJson(data)
            val bytes = json.toByteArray(Charsets.UTF_8)
            if (bytes.size > MAX_BACKUP_BYTES) {
                android.util.Log.w("ProYou", "Backup exceeds ${MAX_BACKUP_BYTES} bytes (${bytes.size}); refusing upload")
                return@withContext Outcome.Failure(IllegalStateException("Backup too large (${bytes.size / 1024} KB)"))
            }
            val content = ByteArrayContent(MIME_TYPE, bytes)

            val existingFileId = findBackupFileId(driveService)
            if (existingFileId != null) {
                driveService.files().update(existingFileId, null, content).execute()
            } else {
                val fileMetadata = File().apply {
                    name = BACKUP_FILENAME
                    parents = listOf("appDataFolder")
                }
                driveService.files().create(fileMetadata, content)
                    .setFields("id")
                    .execute()
            }
            android.util.Log.d("ProYou", "Drive upload succeeded (${bytes.size} bytes)")
            Outcome.Ok(Unit)
        } catch (e: UserRecoverableAuthIOException) {
            android.util.Log.w("ProYou", "Drive upload requires user re-auth", e)
            Outcome.AuthRequired
        } catch (e: Exception) {
            android.util.Log.e("ProYou", "Drive upload failed", e)
            Outcome.Failure(e)
        }
    }

    suspend fun downloadBackup(): Outcome<BackupData> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService() ?: return@withContext Outcome.NotSignedIn
            val fileId = findBackupFileId(driveService) ?: return@withContext Outcome.NotFound

            val outputStream = ByteArrayOutputStream()
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            val json = outputStream.toString("UTF-8")

            val parsed = try {
                gson.fromJson(json, BackupData::class.java)
            } catch (e: JsonSyntaxException) {
                android.util.Log.e("ProYou", "Backup JSON malformed", e)
                return@withContext Outcome.Failure(e)
            } ?: return@withContext Outcome.Failure(IllegalStateException("Backup parsed to null"))
            Outcome.Ok(parsed)
        } catch (e: UserRecoverableAuthIOException) {
            android.util.Log.w("ProYou", "Drive download requires user re-auth", e)
            Outcome.AuthRequired
        } catch (e: Exception) {
            android.util.Log.e("ProYou", "Drive download failed", e)
            Outcome.Failure(e)
        }
    }

    suspend fun getRemoteBackupTimestamp(): Long? = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService() ?: return@withContext null
            val fileId = findBackupFileId(driveService) ?: return@withContext null
            val file = driveService.files().get(fileId)
                .setFields("modifiedTime")
                .execute()
            file.modifiedTime?.value
        } catch (e: UserRecoverableAuthIOException) {
            android.util.Log.w("ProYou", "Drive timestamp fetch requires user re-auth", e)
            null
        } catch (e: Exception) {
            android.util.Log.e("ProYou", "Drive timestamp fetch failed", e)
            null
        }
    }

    private fun findBackupFileId(driveService: Drive): String? {
        // Filename is a const but use Drive's safe-quoting (escape single quotes) defensively
        // in case the constant ever changes to be user-derived.
        val safeName = BACKUP_FILENAME.replace("\\", "\\\\").replace("'", "\\'")
        val result = driveService.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$safeName'")
            .setFields("files(id, name)")
            .setPageSize(1)
            .execute()
        return result.files?.firstOrNull()?.id
    }
}
