package com.example.biometricdemo

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private var keyguardManager: KeyguardManager? = null
    private var biometricManager: BiometricManager? = null
    private var promptInfo: BiometricPrompt.PromptInfo? = null
    private var executor: Executor? = null
    private var biometricPrompt: BiometricPrompt? = null
    private var mScreenLockResultLauncher: ActivityResultLauncher<Intent>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initial()
        loginFingerAuth()
        getBiometricType()
    }

    private fun initial() {
        keyguardManager = this.getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        biometricManager = BiometricManager.from(this)
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor!!, object : AuthenticationCallback() {
            override fun onAuthenticationError(
                errorCode: Int, errString: CharSequence
            ) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(
                    applicationContext, "Authentication error xxxx: $errString", Toast.LENGTH_SHORT
                ).show()
            }

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(
                    applicationContext, "Authentication succeeded!!!", Toast.LENGTH_SHORT
                ).show()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(
                    applicationContext, "Authentication failed", Toast.LENGTH_SHORT
                ).show()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for my app")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Cancel")
            .build()

        // 因为startActivityForResult已弃用，
        // 所以改成registerForActivityResult方法来注册锁屏输入成果的回调监听
        mScreenLockResultLauncher = this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "*** 锁屏暗码验证成功 ***", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "*** 锁屏暗码验证失败 ***", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginFingerAuth() {
        val biometricLoginButton = findViewById<Button>(R.id.biometric_login_finger)
        biometricLoginButton.setOnClickListener {
            if (isBiometricSupport()) {
                doBiometricAuth()
            } else if (isDeviceSecure()) {
                 //根据Android版别判别运用的方法
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    showDeviceLockVerify()
                } else {
                    showScreenLockPwd()
                }
            } else {
                Toast.makeText(this, "Encryption Not Support", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getBiometricType() {
        val biometricTypeButton = findViewById<Button>(R.id.biometric_type)
        biometricTypeButton.setOnClickListener {
            if (isBiometricSupport()) {
                val biometricTypeList = getEnrolledBiometrics()
                findViewById<TextView>(R.id.tvEnrolledType).text = "$biometricTypeList"
            } else {
                Toast.makeText(this, "Biometric Not Support", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun doBiometricAuth() {
        when (biometricManager?.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.d("MY_APP_TAG", "App can authenticate using biometrics.")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val cipher = BiometricCryptoObjectHelper().createCipher(true)
                    biometricPrompt?.authenticate(
                        promptInfo!!, BiometricPrompt.CryptoObject(cipher)
                    )
                }
            }
            else -> {
                Log.d("MY_APP_TAG", "App can not authenticate using biometrics.")
                Toast.makeText(
                    applicationContext, "Authentication UnAble", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    /**
     * 跳转锁屏暗码校验页面（Android 6 以下调用）
     */
    private fun showScreenLockPwd() {
        val intent = keyguardManager?.createConfirmDeviceCredentialIntent("锁屏暗码", "请输入锁屏暗码验证您的身份")
        if (intent != null) {
            // 打开锁屏暗码验证页面
            mScreenLockResultLauncher?.launch(intent)
        } else {
            Toast.makeText(this, "*** 当前设备暂未设置锁屏暗码 ***", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 跳转锁屏暗码验证页面（Android 6 及以上调用）
     */
    private fun showDeviceLockVerify() {
        val executor = ContextCompat.getMainExecutor(this)
        // 创立biometricPrompt以接纳成果的回调
        val biometricPrompt = BiometricPrompt(this, executor, object : AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(applicationContext, "xxx 锁屏暗码验证成功 xxx", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // 这儿的判别是因为当用户点击撤销按钮的时分，不需求提示，其余的验证失败都需求提示
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errString.toString().isNotEmpty()) {
                    Toast.makeText(applicationContext, "xxx 锁屏暗码验证失败: $errString xxx", Toast
                        .LENGTH_SHORT).show()
                }
            }
        })
        // 创立锁屏暗码输入的弹框以及需求显示的文字
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setDeviceCredentialAllowed(true)
            .setTitle("锁屏暗码")
            .setConfirmationRequired(true)
            .setSubtitle("请输入锁屏暗码验证您的身份")
            .build()
        // 发起验证
        biometricPrompt.authenticate(promptInfo)
    }

    private fun isDeviceSecure(): Boolean {
        if (keyguardManager == null) return false
        val isDeviceSecure = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && keyguardManager!!.isDeviceSecure
        Log.e("macy777", "isDeviceSecure : $isDeviceSecure")
        return isDeviceSecure
    }

    private fun isKeyguardSecure(): Boolean {
        if (keyguardManager == null) return false
        val isKeyguardSecure = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && keyguardManager!!.isKeyguardSecure
        Log.e("macy777", "isKeyguardSecure : $isKeyguardSecure")
        return isKeyguardSecure
    }

    private fun isBiometricSupport(): Boolean {
        if (biometricManager == null) return false
        val canAuthenticate = biometricManager!!.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
        Log.e("macy777", "hasBiometricHardware  : $canAuthenticate")
        return canAuthenticate != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE && canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun isDeviceCredentialSupport(): Boolean {
        if (biometricManager == null) return false
        val canAuthenticate = biometricManager!!.canAuthenticate(DEVICE_CREDENTIAL)
        Log.e("macy777", "isDeviceCredentialSupport  : $canAuthenticate")
        return canAuthenticate != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE && canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * 自定义返回类型 理论上没啥用处
     * ps: 其实只要 isBiometricSupport()方法返回 true 就可以调用生物识别认证了
     */
    private fun getEnrolledBiometrics(): List<BiometricType> {
        val biometrics: ArrayList<BiometricType> = ArrayList()
        if (biometricManager == null) return biometrics
        if (biometricManager!!.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
            Log.e("macy777", "add BiometricManager.Authenticators.BIOMETRIC_WEAK")
            biometrics.add(BiometricType.WEAK)
        }
        if (biometricManager!!.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            Log.e("macy777", "add BiometricManager.Authenticators.BIOMETRIC_STRONG")
            biometrics.add(BiometricType.STRONG)
        }
        return biometrics
    }
}