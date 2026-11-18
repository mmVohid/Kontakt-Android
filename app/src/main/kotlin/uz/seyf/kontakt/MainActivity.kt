package uz.seyf.kontakt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import uz.seyf.kontakt.ui.SeyfApp

class MainActivity : FragmentActivity() {
    private val vm: AppViewModel by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private val graceLock = Runnable { vm.lock() }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handler.removeCallbacks(graceLock)
            vm.lock()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // MAJBURIY ekran himoyasi: skrinshot, ekran yozish va "So'nggi ilovalar" ko'rinishi taqiqlanadi.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(false)

        // Boshqa oyna ilova ustini yopib turganda (tapjacking) bosishlarni o'tkazmaydi.
        window.decorView.filterTouchesWhenObscured = true

        ContextCompat.registerReceiver(
            this, screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        setContent {
            SeyfApp(
                vm = vm,
                onBioUnlock = { Biometrics.unlock(this, vm) },
                onBioToggle = { enable ->
                    if (enable) Biometrics.enable(this, vm) else vm.disableBio()
                },
            )
        }

        // Biometrik so'rovni bloklangan holat ochilganda avtomatik ko'rsatish (har bloklashda bir marta).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                vm.state
                    .map { it.phase == Phase.Locked && it.bioEnabled && it.lockRemainingSec == 0 }
                    .distinctUntilChanged()
                    .collect { want ->
                        if (want && vm.consumeAutoBio()) Biometrics.unlock(this@MainActivity, vm)
                    }
            }
        }

        // Harakatsizlik bo'yicha avto-bloklash.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(3_000)
                    vm.checkIdle()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        handler.removeCallbacks(graceLock)
        vm.refreshBio()
    }

    override fun onStop() {
        super.onStop()
        if (isChangingConfigurations) return
        if (vm.externalFlow) {
            // Rasm tanlash oynasi ochiq: 60 soniyagacha kutamiz, keyin baribir bloklaymiz.
            handler.postDelayed(graceLock, GRACE_MS)
        } else {
            vm.lock()
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        vm.poke()
    }

    override fun onDestroy() {
        handler.removeCallbacks(graceLock)
        unregisterReceiver(screenOffReceiver)
        if (!isChangingConfigurations) vm.lock()
        super.onDestroy()
    }

    private companion object {
        const val GRACE_MS = 60_000L
    }
}

/** BiometricPrompt yordamchilari. Kalit biometrika bilan bog'langan Cipher orqali ishlatiladi. */
object Biometrics {
    private fun info(title: String, negative: String) = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle("Seyf Kontakt")
        .setNegativeButtonText(negative)
        .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .build()

    private fun isCancel(code: Int) = code == BiometricPrompt.ERROR_USER_CANCELED ||
        code == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
        code == BiometricPrompt.ERROR_CANCELED

    fun unlock(activity: FragmentActivity, vm: AppViewModel) {
        val cipher = vm.bioUnlockCipher() ?: return
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    result.cryptoObject?.cipher?.let { vm.onBioUnlocked(it) }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Foydalanuvchi bekor qilsa, PIN bilan davom etadi.
                }
            },
        )
        prompt.authenticate(info("Biometrik kirish", "PIN kiritish"), BiometricPrompt.CryptoObject(cipher))
    }

    fun enable(activity: FragmentActivity, vm: AppViewModel) {
        val cipher = try {
            vm.bioEnableCipher()
        } catch (e: Exception) {
            vm.toast("Biometrikani yoqib bo'lmadi")
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    result.cryptoObject?.cipher?.let { vm.onBioEnabled(it) }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (!isCancel(errorCode)) vm.toast(errString.toString())
                }
            },
        )
        prompt.authenticate(info("Biometrik kirishni yoqish", "Bekor qilish"), BiometricPrompt.CryptoObject(cipher))
    }
}
