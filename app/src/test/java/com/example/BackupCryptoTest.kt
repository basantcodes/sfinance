package com.example

import com.example.data.backup.BackupCrypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BackupCryptoTest {
    @Test
    fun `encrypted backup round trips and wrong passphrase fails authentication`() {
        val plainText = "sensitive finance record".toByteArray()
        val encrypted = BackupCrypto.encrypt(plainText, "a sufficiently long passphrase", BackupCrypto.keySalt())
        assertNotEquals(String(plainText), String(encrypted.bytes))
        assertArrayEquals(
            plainText,
            BackupCrypto.decrypt(encrypted.bytes, "a sufficiently long passphrase", BackupCrypto.keySalt(), encrypted.iv)
        )
        org.junit.Assert.assertThrows(Exception::class.java) {
            BackupCrypto.decrypt(encrypted.bytes, "a different passphrase", BackupCrypto.keySalt(), encrypted.iv)
        }
    }
}