package com.example.data.backup

import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

class DriveBackupClient(private val context: Context) {
    private companion object {
        const val DRIVE_APP_DATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val binaryMediaType = "application/octet-stream".toMediaType()

    suspend fun upload(account: GoogleSignInAccount, bytes: ByteArray): Unit = withContext(Dispatchers.IO) {
        val token = accessToken(account)
        val existingId = findBackup(token)
        val metadata = JSONObject().apply {
            put("name", BACKUP_FILE_NAME)
            put("mimeType", "application/octet-stream")
            if (existingId == null) put("parents", org.json.JSONArray().put("appDataFolder"))
        }.toString().toRequestBody(jsonMediaType)
        val content = bytes.toRequestBody(binaryMediaType)
        val body = MultipartBody.Builder("backup-boundary")
            .setType("multipart/related".toMediaType())
            .addPart(metadata)
            .addPart(content)
            .build()
        val url = if (existingId == null) {
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
        } else {
            "https://www.googleapis.com/upload/drive/v3/files/$existingId?uploadType=multipart"
        }
        val request = Request.Builder().url(url).header("Authorization", "Bearer $token")
            .method(if (existingId == null) "POST" else "PATCH", body).build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Drive upload failed (${response.code})" }
        }
        check(findBackup(token) != null) { "Drive upload could not be verified" }
    }

    suspend fun download(account: GoogleSignInAccount): ByteArray? = withContext(Dispatchers.IO) {
        val token = accessToken(account)
        val id = findBackup(token) ?: return@withContext null
        val request = Request.Builder().url("https://www.googleapis.com/drive/v3/files/$id?alt=media")
            .header("Authorization", "Bearer $token").build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Drive download failed (${response.code})" }
            response.body?.bytes() ?: error("Drive backup was empty")
        }
    }

    private fun findBackup(token: String): String? {
        val query = "'appDataFolder' in parents and name = '$BACKUP_FILE_NAME' and trashed = false"
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?q=$encoded&spaces=appDataFolder&fields=files(id)")
            .header("Authorization", "Bearer $token").build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Drive lookup failed (${response.code})" }
            val files = JSONObject(response.body?.string().orEmpty()).optJSONArray("files")
            return files?.takeIf { it.length() > 0 }?.getJSONObject(0)?.optString("id")
        }
    }

    private fun accessToken(account: GoogleSignInAccount): String {
        val googleAccount = requireNotNull(account.account) { "Google account is unavailable" }
        return GoogleAuthUtil.getToken(context, googleAccount, "oauth2:$DRIVE_APP_DATA_SCOPE")
    }
}
