// ARMOR-ANDROID-CONTROL - the phone's own question (fingerprint, face, PIN, pattern or password) put to the person, with what the Android framework offers and nothing else to install.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
//
// From Android 10 the system's biometric prompt is used, with the phone's PIN as the way out of it (from Android 11 a fingerprint or a face of the "weak" class is accepted as well as the strong
// ones, which is what a phone's face unlock usually is); on Android 8 and 9 the phone's own "confirm your PIN, pattern or password" screen is used. The rules of when to ask are in DeviceLock.kt.
package es.electrohobby3d.armor

import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** The error codes of the system's prompt that matter here (the framework's constants are not all public on every version). */
private object PromptError {
    const val HW_UNAVAILABLE = 1
    const val CANCELED = 5
    const val USER_CANCELED = 10
    const val NO_BIOMETRICS = 11
    const val HW_NOT_PRESENT = 12
    const val NEGATIVE_BUTTON = 13
    const val NO_DEVICE_CREDENTIAL = 14

    fun result(code: Int, message: CharSequence?): AuthResult = when (code) {
        CANCELED, USER_CANCELED, NEGATIVE_BUTTON -> AuthResult.Cancelled
        HW_UNAVAILABLE, NO_BIOMETRICS, HW_NOT_PRESENT, NO_DEVICE_CREDENTIAL -> AuthResult.Unavailable
        else -> AuthResult.Failed(message?.toString().orEmpty())
    }
}

/** Must be created while the activity is being created (it registers for the answer of the older "confirm your PIN" screen). */
class AndroidDeviceAuth(private val activity: ComponentActivity) : DeviceAuth {
    private var pending: ((AuthResult) -> Unit)? = null
    private val credentialScreen: ActivityResultLauncher<android.content.Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            pending?.invoke(if (result.resultCode == android.app.Activity.RESULT_OK) AuthResult.Success else AuthResult.Cancelled)
            pending = null
        }

    override fun availability(): LockAvailability {
        val keyguard = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val secure = keyguard?.isDeviceSecure == true
        if (Build.VERSION.SDK_INT >= 30) {
            val manager = activity.getSystemService(BiometricManager::class.java)
            if (manager?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) return LockAvailability.Biometric
        } else if (Build.VERSION.SDK_INT >= 29) {
            @Suppress("DEPRECATION")
            val manager = activity.getSystemService(BiometricManager::class.java)
            @Suppress("DEPRECATION")
            if (manager?.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) return LockAvailability.Biometric
        }
        return if (secure) LockAvailability.DeviceCredential else LockAvailability.None
    }

    override suspend fun authenticate(title: String, subtitle: String): AuthResult = suspendCancellableCoroutine { continuation ->
        val finish: (AuthResult) -> Unit = { if (continuation.isActive) continuation.resume(it) }
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val builder = BiometricPrompt.Builder(activity).setTitle(title).setSubtitle(subtitle)
                if (Build.VERSION.SDK_INT >= 30) builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                else { @Suppress("DEPRECATION") builder.setDeviceCredentialAllowed(true) }
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                builder.build().authenticate(signal, activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) = finish(AuthResult.Success)
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) = finish(PromptError.result(errorCode, errString))
                })
            } else {
                val keyguard = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                @Suppress("DEPRECATION")
                val intent = keyguard.createConfirmDeviceCredentialIntent(title, subtitle)
                if (intent == null) finish(AuthResult.Unavailable)
                else { pending = finish; credentialScreen.launch(intent) }
            }
        } catch (error: Exception) {
            finish(AuthResult.Unavailable)
        }
    }
}
