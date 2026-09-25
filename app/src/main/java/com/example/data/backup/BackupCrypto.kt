package com.example.data.backup

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_LENGTH_BYTES = 32

    data class Ciphertext(val bytes: ByteArray, val iv: ByteArray)

    fun encrypt(plainText: ByteArray, passphrase: CharSequence, salt: ByteArray): Ciphertext {
        val key = deriveKey(passphrase, salt)
        val iv = ByteArray(12).also { java.security.SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        return Ciphertext(cipher.doFinal(plainText), iv)
    }

    fun decrypt(cipherText: ByteArray, passphrase: CharSequence, salt: ByteArray, iv: ByteArray): ByteArray {
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(cipherText)
    }

    fun keySalt(): ByteArray = "sfinance-backup-v1".toByteArray(Charsets.UTF_8)
    fun newSalt(): ByteArray = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }

    fun encode(value: ByteArray): String = Base64.encodeToString(value, Base64.NO_WRAP)
    fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

    private fun deriveKey(passphrase: CharSequence, salt: ByteArray): SecretKeySpec {
        var digest = passphrase.toString().toByteArray(Charsets.UTF_8) + salt
        repeat(100_000) {
            digest = MessageDigest.getInstance("SHA-256").digest(digest)
        }
        return SecretKeySpec(digest.copyOf(KEY_LENGTH_BYTES), "AES")
    }
}
