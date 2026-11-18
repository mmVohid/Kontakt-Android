package uz.seyf.kontakt

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import android.os.SystemClock
import android.util.LruCache
import androidx.biometric.BiometricManager
import androidx.core.content.FileProvider
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import uz.seyf.kontakt.data.Contact
import uz.seyf.kontakt.data.PhotoUtils
import uz.seyf.kontakt.security.AttemptGuard
import uz.seyf.kontakt.security.Backup
import uz.seyf.kontakt.security.Vault
import java.io.File
import java.io.RandomAccessFile
import javax.crypto.Cipher
import kotlin.math.ceil

enum class Phase { Setup, Locked, Unlocked }

/** Snackbar hodisasi. [onAction] bosilsa (masalan, "Bekor qilish") chaqiriladi. */
data class UiEvent(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

sealed interface Route {
    data object Home : Route
    data class Detail(val id: String) : Route
    data class Edit(val id: String?) : Route
    data object Settings : Route
    data object ChangePin : Route
}

data class UiState(
    val phase: Phase,
    val route: Route = Route.Home,
    val contacts: List<Contact> = emptyList(),
    val query: String = "",
    val busy: Boolean = false,
    val working: Boolean = false,
    val pinError: String? = null,
    val shake: Int = 0,
    val lockRemainingSec: Int = 0,
    val bioAvailable: Boolean = false,
    val bioEnabled: Boolean = false,
    val offerBio: Boolean = false,
    val idleTimeoutSec: Int = 120,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val vault = Vault(app)
    private val guard = AttemptGuard(app)
    private val prefs = app.getSharedPreferences("seyf_settings", Context.MODE_PRIVATE)
    private val ioMutex = Mutex()
    private val photoCache = LruCache<String, ImageBitmap>(48)

    private val _state = MutableStateFlow(
        UiState(
            phase = if (vault.isSetUp) Phase.Locked else Phase.Setup,
            idleTimeoutSec = prefs.getInt(KEY_IDLE, 120),
            lockRemainingSec = secondsOf(guard.remainingLockMs()),
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private var pendingDelete: Pair<Contact, ByteArray?>? = null
    private var captureFile: File? = null

    /** Rasm tanlash kabi tashqi oynalar ochilganda true. onStop'da darhol bloklashni kechiktiradi. */
    @Volatile
    var externalFlow: Boolean = false

    @Volatile
    private var lastActivity = SystemClock.elapsedRealtime()
    private var autoBioDone = false

    init {
        refreshBio()
        viewModelScope.launch {
            while (true) {
                delay(500)
                val sec = secondsOf(guard.remainingLockMs())
                val cur = _state.value
                if (sec != cur.lockRemainingSec) {
                    _state.update {
                        it.copy(
                            lockRemainingSec = sec,
                            pinError = if (sec == 0 && it.phase != Phase.Unlocked) null else it.pinError,
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        vault.lock()
        super.onCleared()
    }

    // ---------- Biometrika holati ----------

    fun refreshBio() {
        val ctx = getApplication<Application>()
        val available = BiometricManager.from(ctx)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        val enabled = available && vault.biometricEnabled()
        _state.update { it.copy(bioAvailable = available, bioEnabled = enabled) }
    }

    fun consumeAutoBio(): Boolean {
        if (autoBioDone) return false
        autoBioDone = true
        return true
    }

    fun bioUnlockCipher(): Cipher? {
        if (guard.remainingLockMs() > 0) return null
        val c = vault.bioUnlockCipher()
        if (c == null) {
            val still = vault.biometricEnabled()
            _state.update {
                it.copy(
                    bioEnabled = still,
                    pinError = if (!still && it.bioEnabled) "Biometrik ma'lumot o'zgargan. PIN bilan kiring." else it.pinError,
                )
            }
        }
        return c
    }

    fun onBioUnlocked(cipher: Cipher) {
        viewModelScope.launch(Dispatchers.Default) {
            if (vault.bioUnlockFinish(cipher)) {
                guard.onSuccess()
                finishUnlock()
            } else {
                _state.update { it.copy(pinError = "Biometrik kirish amalga oshmadi. PIN kiriting.") }
            }
        }
    }

    fun bioEnableCipher(): Cipher = vault.bioEnableCipher()

    fun onBioEnabled(cipher: Cipher) {
        try {
            vault.bioEnableFinish(cipher)
            _state.update { it.copy(bioEnabled = true, offerBio = false) }
            toast("Biometrik kirish yoqildi")
        } catch (e: Exception) {
            vault.disableBiometric()
            _state.update { it.copy(bioEnabled = false) }
            toast("Biometrikani yoqib bo'lmadi")
        }
    }

    fun disableBio() {
        vault.disableBiometric()
        _state.update { it.copy(bioEnabled = false) }
        toast("Biometrik kirish o'chirildi")
    }

    fun dismissBioOffer() = _state.update { it.copy(offerBio = false) }

    // ---------- PIN ----------

    fun setPin(pin: String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, pinError = null) }
        viewModelScope.launch(Dispatchers.Default) {
            val chars = pin.toCharArray()
            try {
                vault.setupPin(chars)
                guard.onSuccess()
                autoBioDone = true
                _state.update {
                    it.copy(
                        phase = Phase.Unlocked, busy = false, route = Route.Home,
                        contacts = emptyList(), offerBio = it.bioAvailable, pinError = null,
                    )
                }
                lastActivity = SystemClock.elapsedRealtime()
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, pinError = "Seyfni yaratib bo'lmadi (${e.javaClass.simpleName})") }
            } finally {
                chars.fill('\u0000')
            }
        }
    }

    fun tryUnlock(pin: String) {
        if (guard.remainingLockMs() > 0 || _state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch(Dispatchers.Default) {
            val chars = pin.toCharArray()
            val ok = try {
                vault.unlockWithPin(chars)
            } finally {
                chars.fill('\u0000')
            }
            if (ok) {
                guard.onSuccess()
                finishUnlock()
            } else {
                registerFailure()
            }
        }
    }

    /** Joriy PINni tekshiradi (PIN almashtirish uchun). */
    fun checkCurrentPin(pin: String, onResult: (Boolean) -> Unit) {
        if (guard.remainingLockMs() > 0 || _state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val chars = pin.toCharArray()
            val ok = withContext(Dispatchers.Default) {
                try {
                    vault.verifyPin(chars)
                } finally {
                    chars.fill('\u0000')
                }
            }
            if (ok) {
                guard.onSuccess()
                _state.update { it.copy(busy = false, pinError = null) }
            } else {
                registerFailure()
            }
            onResult(ok)
        }
    }

    fun changePin(oldPin: String, newPin: String) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val o = oldPin.toCharArray()
            val n = newPin.toCharArray()
            val ok = withContext(Dispatchers.Default) {
                try {
                    vault.changePin(o, n)
                } finally {
                    o.fill('\u0000')
                    n.fill('\u0000')
                }
            }
            _state.update { it.copy(busy = false, route = if (ok) Route.Settings else it.route) }
            toast(if (ok) "PIN o'zgartirildi" else "PINni o'zgartirib bo'lmadi")
        }
    }

    private fun registerFailure() {
        guard.onFailure()
        val remMs = guard.remainingLockMs()
        val left = guard.attemptsLeft()
        val msg = when {
            remMs > 0 -> "Juda ko'p urinish. Biroz kuting."
            left > 0 -> "PIN noto'g'ri. Qolgan urinish: $left"
            else -> "PIN noto'g'ri"
        }
        _state.update {
            it.copy(busy = false, pinError = msg, shake = it.shake + 1, lockRemainingSec = secondsOf(remMs))
        }
    }

    private suspend fun finishUnlock() {
        val loaded = try {
            Result.success(vault.loadContacts())
        } catch (e: Exception) {
            Result.failure(e)
        }
        loaded.onSuccess { list ->
            lastActivity = SystemClock.elapsedRealtime()
            _state.update {
                it.copy(
                    phase = Phase.Unlocked, busy = false, pinError = null, route = Route.Home, query = "",
                    contacts = list.sortedWith(Contact.ORDER), lockRemainingSec = 0,
                )
            }
        }.onFailure {
            vault.lock()
            _state.update {
                it.copy(busy = false, pinError = "Ma'lumotni o'qib bo'lmadi (fayl shikastlangan bo'lishi mumkin)")
            }
        }
    }

    // ---------- Bloklash ----------

    fun lock() {
        if (!vault.isSetUp) return
        vault.lock()
        photoCache.evictAll()
        pendingDelete = null
        wipeCaptureDir()
        autoBioDone = false
        externalFlow = false
        _state.update {
            if (it.phase == Phase.Setup) it
            else it.copy(
                phase = Phase.Locked, route = Route.Home, contacts = emptyList(), query = "",
                busy = false, pinError = null, offerBio = false,
            )
        }
    }

    fun poke() {
        lastActivity = SystemClock.elapsedRealtime()
    }

    fun checkIdle() {
        val s = _state.value
        if (s.phase != Phase.Unlocked || s.idleTimeoutSec <= 0 || externalFlow) return
        if (SystemClock.elapsedRealtime() - lastActivity > s.idleTimeoutSec * 1000L) lock()
    }

    fun setIdleTimeout(sec: Int) {
        prefs.edit().putInt(KEY_IDLE, sec).apply()
        _state.update { it.copy(idleTimeoutSec = sec) }
    }

    /** PIN unutilganda: butun omborni o'chiradi va qaytadan sozlashga o'tadi. */
    fun wipeAll() {
        viewModelScope.launch(Dispatchers.IO) {
            vault.wipe()
            guard.onSuccess()
            photoCache.evictAll()
            autoBioDone = true
            _state.update {
                UiState(
                    phase = Phase.Setup, idleTimeoutSec = it.idleTimeoutSec,
                    bioAvailable = it.bioAvailable, bioEnabled = false,
                )
            }
        }
    }

    // ---------- Navigatsiya ----------

    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun openContact(id: String) = _state.update { it.copy(route = Route.Detail(id)) }
    fun newContact() = _state.update { it.copy(route = Route.Edit(null)) }
    fun editContact(id: String) = _state.update { it.copy(route = Route.Edit(id)) }
    fun openSettings() = _state.update { it.copy(route = Route.Settings) }
    fun openChangePin() = _state.update { it.copy(route = Route.ChangePin, pinError = null, shake = 0) }

    fun back() = _state.update {
        val next = when (val r = it.route) {
            Route.Home -> Route.Home
            is Route.Detail -> Route.Home
            is Route.Edit -> if (r.id != null) Route.Detail(r.id) else Route.Home
            Route.Settings -> Route.Home
            Route.ChangePin -> Route.Settings
        }
        it.copy(route = next, pinError = null)
    }

    // ---------- Kontaktlar ----------

    fun saveContact(draft: Contact, newPhoto: ByteArray?, removePhoto: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            ioMutex.withLock {
                try {
                    val current = _state.value.contacts
                    val existing = current.find { it.id == draft.id }
                    val hasPhoto = when {
                        newPhoto != null -> true
                        removePhoto -> false
                        else -> existing?.hasPhoto ?: false
                    }
                    if (newPhoto != null) vault.savePhoto(draft.id, newPhoto)
                    else if (removePhoto) vault.deletePhoto(draft.id)

                    val now = System.currentTimeMillis()
                    val saved = draft.copy(
                        hasPhoto = hasPhoto,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now,
                    )
                    val updated = (current.filter { it.id != saved.id } + saved).sortedWith(Contact.ORDER)
                    vault.saveContacts(updated)
                    photoCache.evictAll()
                    _state.update { it.copy(contacts = updated, route = Route.Detail(saved.id)) }
                    toast("Saqlandi")
                } catch (e: Exception) {
                    toast("Saqlab bo'lmadi: ${e.javaClass.simpleName}")
                }
            }
        }
    }

    fun deleteContact(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            ioMutex.withLock {
                try {
                    val target = _state.value.contacts.find { it.id == id } ?: return@withLock
                    val photoBytes = if (target.hasPhoto) vault.loadPhoto(id) else null
                    val updated = _state.value.contacts.filter { it.id != id }
                    vault.saveContacts(updated)
                    vault.deletePhoto(id)
                    photoCache.evictAll()
                    pendingDelete = target to photoBytes
                    _state.update { it.copy(contacts = updated, route = Route.Home) }
                    _events.tryEmit(UiEvent("Kontakt o'chirildi", "Bekor qilish") { undoDelete() })
                } catch (e: Exception) {
                    toast("O'chirib bo'lmadi: ${e.javaClass.simpleName}")
                }
            }
        }
    }

    private fun undoDelete() {
        val (contact, photoBytes) = pendingDelete ?: return
        pendingDelete = null
        viewModelScope.launch(Dispatchers.IO) {
            ioMutex.withLock {
                try {
                    if (photoBytes != null) vault.savePhoto(contact.id, photoBytes)
                    val updated = (_state.value.contacts.filter { it.id != contact.id } + contact)
                        .sortedWith(Contact.ORDER)
                    vault.saveContacts(updated)
                    photoCache.evictAll()
                    _state.update { it.copy(contacts = updated) }
                    toast("Kontakt qaytarildi")
                } catch (e: Exception) {
                    toast("Qaytarib bo'lmadi: ${e.javaClass.simpleName}")
                }
            }
        }
    }

    /** Shifrlangan rasmni o'qib, ekran uchun kichraytirib beradi (keshlanadi). */
    suspend fun photo(id: String, maxPx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        val key = "$id@$maxPx"
        photoCache.get(key)?.let { return@withContext it }
        try {
            val bytes = vault.loadPhoto(id) ?: return@withContext null
            PhotoUtils.decodeSampled(bytes, maxPx)?.also { photoCache.put(key, it) }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun processPhoto(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        try {
            PhotoUtils.prepareAvatar(getApplication<Application>(), uri)
        } catch (e: Exception) {
            null
        }
    }

    // ---------- Kamera ----------

    /** Kamera rasmi yoziladigan vaqtinchalik fayl uchun URI (FileProvider orqali). */
    fun prepareCapture(): Uri? = try {
        val app = getApplication<Application>()
        val dir = File(app.cacheDir, "capture").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val f = File(dir, "cap_${System.currentTimeMillis()}.jpg")
        captureFile = f
        FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", f)
    } catch (e: Exception) {
        null
    }

    /** Kamera natijasini kvadrat JPEG'ga aylantiradi va vaqtinchalik faylni nollar bilan yozib o'chiradi. */
    suspend fun processCapture(ok: Boolean): ByteArray? = withContext(Dispatchers.IO) {
        val f = captureFile
        captureFile = null
        try {
            if (!ok || f == null || !f.exists() || f.length() == 0L) null
            else PhotoUtils.prepareAvatar(getApplication<Application>(), Uri.fromFile(f))
        } catch (e: Exception) {
            null
        } finally {
            f?.let { shred(it) }
        }
    }

    private fun shred(f: File) {
        try {
            if (f.exists()) {
                RandomAccessFile(f, "rw").use { raf ->
                    val zeros = ByteArray(8192)
                    var left = raf.length()
                    raf.seek(0)
                    while (left > 0) {
                        val n = minOf(left, zeros.size.toLong()).toInt()
                        raf.write(zeros, 0, n)
                        left -= n
                    }
                    raf.fd.sync()
                }
            }
        } catch (_: Exception) {
        } finally {
            f.delete()
        }
    }

    private fun wipeCaptureDir() {
        captureFile = null
        File(getApplication<Application>().cacheDir, "capture").listFiles()?.forEach { shred(it) }
    }

    // ---------- Zaxira nusxa ----------

    fun exportBackup(uri: Uri, password: String) {
        if (_state.value.working) return
        _state.update { it.copy(working = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val pwd = password.toCharArray()
            try {
                val contacts = ioMutex.withLock { _state.value.contacts }
                val bytes = Backup.build(contacts, { id -> runCatching { vault.loadPhoto(id) }.getOrNull() }, pwd)
                val out = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                    ?: throw IllegalStateException("Fayl ochilmadi")
                out.use { it.write(bytes) }
                toast("Zaxira nusxa saqlandi (${contacts.size} ta kontakt)")
            } catch (e: Exception) {
                toast("Zaxira nusxa yaratib bo'lmadi: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                pwd.fill('\u0000')
                _state.update { it.copy(working = false) }
            }
        }
    }

    fun importBackup(uri: Uri, password: String) {
        if (_state.value.working) return
        _state.update { it.copy(working = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val pwd = password.toCharArray()
            try {
                val data = getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw Backup.BadBackupException("Faylni o'qib bo'lmadi")
                if (data.size > MAX_BACKUP_BYTES) throw Backup.BadBackupException("Fayl juda katta")
                val parsed = Backup.parse(data, pwd)

                ioMutex.withLock {
                    val byId = _state.value.contacts.associateBy { it.id }.toMutableMap()
                    var added = 0
                    var updatedCount = 0
                    for (c in parsed.contacts) {
                        val existing = byId[c.id]
                        val take = existing == null || c.updatedAt > existing.updatedAt
                        if (!take) continue
                        val photo = parsed.photos[c.id]
                        if (photo != null) vault.savePhoto(c.id, photo)
                        byId[c.id] = c.copy(hasPhoto = photo != null)
                        if (existing == null) added++ else updatedCount++
                    }
                    val merged = byId.values.sortedWith(Contact.ORDER)
                    vault.saveContacts(merged)
                    photoCache.evictAll()
                    _state.update { it.copy(contacts = merged) }
                    toast("Tiklandi: $added ta yangi, $updatedCount ta yangilandi")
                }
            } catch (e: Backup.BadBackupException) {
                toast(e.message ?: "Zaxira fayli noto'g'ri")
            } catch (e: Exception) {
                toast("Tiklab bo'lmadi: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                pwd.fill('\u0000')
                _state.update { it.copy(working = false) }
            }
        }
    }

    // ---------- Buferga nusxalash ----------

    fun copy(text: String, label: String) {
        val cm = getApplication<Application>().getSystemService(ClipboardManager::class.java)
        val clip = ClipData.newPlainText(label, text)
        // Android 13+: tarkib bildirishnomada/ko'rinishda yashiriladi
        clip.description.extras = PersistableBundle().apply {
            putBoolean("android.content.extra.IS_SENSITIVE", true)
        }
        cm.setPrimaryClip(clip)
        toast("$label nusxalandi (30 soniyadan keyin tozalanadi)")
        viewModelScope.launch {
            delay(30_000)
            try {
                val now = cm.primaryClip?.getItemAt(0)?.text?.toString()
                if (now == text) {
                    if (Build.VERSION.SDK_INT >= 28) cm.clearPrimaryClip()
                    else cm.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            } catch (_: Exception) {
            }
        }
    }

    fun toast(msg: String) {
        _events.tryEmit(UiEvent(msg))
    }

    private fun secondsOf(ms: Long): Int = ceil(ms / 1000.0).toInt()

    private companion object {
        const val KEY_IDLE = "idle_sec"
        const val MAX_BACKUP_BYTES = 64 * 1024 * 1024
    }
}
