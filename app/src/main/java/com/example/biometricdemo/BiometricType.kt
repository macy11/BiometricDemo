package com.example.biometricdemo

/// Various types of biometric authentication.
/// Some platforms report specific biometric types, while others report only
/// classifications like strong and weak.
enum class BiometricType {
    /// platform API considers to be strong. For example, on Android this
    /// corresponds to Class 3.
    STRONG,  /// Any biometric (e.g. fingerprint, iris, or face) on the device that the

    /// platform API considers to be weak. For example, on Android this
    /// corresponds to Class 2.
    WEAK
}