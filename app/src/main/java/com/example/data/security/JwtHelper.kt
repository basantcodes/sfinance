package com.example.data.security

import android.util.Base64
import org.json.JSONObject
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object JwtHelper {
    private const val SECRET = "personal-finance-journal-secret-key-2026-very-secure-32bytes!"
    private const val HMAC_ALGO = "HmacSHA256"
    private const val THIRTY_DAYS_MILLIS = 30L * 24L * 60L * 60L * 1000L

    data class JwtPayload(
        val userId: String,
        val username: String,
        val name: String,
        val issuedAt: Long,
        val expiresAt: Long
    ) {
        val email: String
            get() = username

        val isExpired: Boolean
            get() = System.currentTimeMillis() > expiresAt
    }

    fun createToken(userId: String, username: String, name: String): String {
        val now = System.currentTimeMillis()
        val exp = now + THIRTY_DAYS_MILLIS

        val headerJson = JSONObject().apply {
            put("alg", "HS256")
            put("typ", "JWT")
        }

        val payloadJson = JSONObject().apply {
            put("userId", userId)
            put("username", username)
            put("name", name)
            put("iat", now)
            put("exp", exp)
        }

        val headerBase64 = base64UrlEncode(headerJson.toString().toByteArray(Charsets.UTF_8))
        val payloadBase64 = base64UrlEncode(payloadJson.toString().toByteArray(Charsets.UTF_8))
        val dataToSign = "$headerBase64.$payloadBase64"
        val signature = sign(dataToSign)

        return "$dataToSign.$signature"
    }

    fun verifyToken(token: String): JwtPayload? {
        try {
            val parts = token.split(".")
            if (parts.size != 3) return null

            val dataToSign = "${parts[0]}.${parts[1]}"
            val expectedSig = sign(dataToSign)
            if (expectedSig != parts[2]) return null

            val payloadBytes = base64UrlDecode(parts[1])
            val payloadJson = JSONObject(String(payloadBytes, Charsets.UTF_8))

            val exp = payloadJson.optLong("exp", 0L)
            if (System.currentTimeMillis() > exp) return null

            return JwtPayload(
                userId = payloadJson.getString("userId"),
                username = payloadJson.optString("username", payloadJson.optString("email")),
                name = payloadJson.optString("name", "User"),
                issuedAt = payloadJson.optLong("iat", 0L),
                expiresAt = exp
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun sign(data: String): String {
        val mac = Mac.getInstance(HMAC_ALGO)
        val secretKey = SecretKeySpec(SECRET.toByteArray(Charsets.UTF_8), HMAC_ALGO)
        mac.init(secretKey)
        val bytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return base64UrlEncode(bytes)
    }

    private fun base64UrlEncode(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun base64UrlDecode(str: String): ByteArray {
        return Base64.decode(str, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
