package com.crazycat.app.crypto

object KeeloqEngine {
    init {
        System.loadLibrary("keeloq_jni")
    }

    private external fun decryptNative(data: Int, key: Long): Int
    private external fun encryptNative(data: Int, key: Long): Int

    fun decryptRollingCode(capturedCode: Int, manufacturerKey: Long): Int {
        return decryptNative(capturedCode, manufacturerKey)
    }

    fun generateNextValidCode(decryptedCode: Int, manufacturerKey: Long): Int {
        // O contador está nos bits 0-15
        val counter = decryptedCode and 0xFFFF
        val nextCounter = counter + 1
        val nextDecrypted = (decryptedCode and 0xFFFF0000.toInt()) or nextCounter
        return encryptNative(nextDecrypted, manufacturerKey)
    }
}
