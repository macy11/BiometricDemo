package com.example.biometricdemo

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.RequiresApi
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

@RequiresApi(Build.VERSION_CODES.M)
class BiometricCryptoObjectHelper {

    private var _keystore: KeyStore

    init {
        Log.e("MY_APP_TAG", "BiometricCryptoObjectHelper init")
        _keystore = KeyStore.getInstance(KEYSTORE_NAME)
        _keystore.load(null)
    }

    private fun getSecretKey(): SecretKey {
        Log.e("MY_APP_TAG", "getSecretKey: " + _keystore.isKeyEntry(KEY_NAME))
        if (!_keystore.isKeyEntry(KEY_NAME)) {
            generateSecretKey()
        }
        return _keystore.getKey(KEY_NAME, null) as SecretKey
    }

    private fun generateSecretKey() {
        Log.e("MY_APP_TAG", "generateSecretKey")
        val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM, KEYSTORE_NAME)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_NAME, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(BLOCK_MODE).setEncryptionPaddings(ENCRYPTION_PADDING).setUserAuthenticationRequired(true)
            .build()
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    fun createCipher(retry: Boolean): Cipher {
        val secretKey = getSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        try {
            cipher.init(Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE, secretKey)
        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.e("MY_APP_TAG", "KeyPermanentlyInvalidatedException $e")
            _keystore.deleteEntry(KEY_NAME)
            if (retry) {
                Log.e("MY_APP_TAG", "retry createCipher")
                return createCipher(false)
            } else {
                throw Exception("Could not create the cipher for fingerprint authentication.", e)
            }
        }
        return cipher
    }

    companion object {
        // This can be key name you want. Should be unique for the app.
        internal const val KEY_NAME = "minefocus.fingerprint_authentication_key"

        // We always use this keystore on Android.
        internal const val KEYSTORE_NAME = "AndroidKeyStore"

        // Should be no need to change these values.
        internal const val KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        internal const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        internal const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
        internal const val TRANSFORMATION = "$KEY_ALGORITHM/$BLOCK_MODE/$ENCRYPTION_PADDING"
    }
}