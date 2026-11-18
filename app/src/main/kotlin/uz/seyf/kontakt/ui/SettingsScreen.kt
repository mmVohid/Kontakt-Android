package uz.seyf.kontakt.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private val IdleOptions = listOf(
    30 to "30 soniya",
    60 to "1 daqiqa",
    120 to "2 daqiqa",
    300 to "5 daqiqa",
)

private val Protections = listOf(
    "Ma'lumotlar AES-256-GCM bilan shifrlanadi",
    "Kalit Android Keystore'da saqlanadi va qurilmadan chiqmaydi",
    "PIN 200 000 marta PBKDF2 orqali kalitga aylantiriladi",
    "Skrinshot, ekran yozish va 'So'nggi ilovalar'da ko'rinish taqiqlangan",
    "Ilovadan chiqilsa yoki ekran o'chsa, avtomatik bloklanadi",
    "Internet ruxsati yo'q: ma'lumot hech qayerga yuborilmaydi",
    "Bulutli zaxira nusxa (backup) o'chirilgan",
    "Noto'g'ri PIN kiritilsa, kutish vaqti oshib boradi",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    bioAvailable: Boolean,
    bioEnabled: Boolean,
    idleTimeoutSec: Int,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onBioToggle: (Boolean) -> Unit,
    onChangePin: () -> Unit,
    onIdleTimeout: (Int) -> Unit,
    onLockNow: () -> Unit,
) {
    var askIdle by remember { mutableStateOf(false) }
    val transparent = ListItemDefaults.colors(containerColor = Color.Transparent)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sozlamalar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            SectionTitle("Kirish")
            Spacer(Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ListItem(
                    colors = transparent,
                    leadingContent = { Icon(Icons.Default.Fingerprint, null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("Biometrik kirish") },
                    supportingContent = {
                        Text(if (bioAvailable) "Barmoq izi yoki yuz bilan tez ochish" else "Qurilmada biometrik ma'lumot sozlanmagan")
                    },
                    trailingContent = {
                        Switch(checked = bioEnabled, onCheckedChange = onBioToggle, enabled = bioAvailable)
                    },
                )
                HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
                ListItem(
                    modifier = Modifier.clickable(onClick = onChangePin),
                    colors = transparent,
                    leadingContent = { Icon(Icons.Default.Pin, null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("PINni o'zgartirish") },
                    supportingContent = { Text("6 xonali kirish kodi") },
                )
                HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant)
                ListItem(
                    modifier = Modifier.clickable { askIdle = true },
                    colors = transparent,
                    leadingContent = { Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("Harakatsizlikda bloklash") },
                    supportingContent = {
                        Text(IdleOptions.firstOrNull { it.first == idleTimeoutSec }?.second ?: "$idleTimeoutSec soniya")
                    },
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("Himoya holati")
            Spacer(Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                    Protections.forEach { text ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.Check, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            FilledTonalButton(onClick = onLockNow, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Hozir bloklash")
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Seyf Kontakt 1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (askIdle) {
        var picked by remember { mutableIntStateOf(idleTimeoutSec) }
        AlertDialog(
            onDismissRequest = { askIdle = false },
            title = { Text("Harakatsizlikda bloklash") },
            text = {
                Column {
                    IdleOptions.forEach { (sec, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(selected = picked == sec, role = Role.RadioButton, onClick = { picked = sec })
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = picked == sec, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { onIdleTimeout(picked); askIdle = false }) { Text("Saqlash") } },
            dismissButton = { TextButton(onClick = { askIdle = false }) { Text("Bekor qilish") } },
        )
    }
}

/** PINni o'zgartirish: joriy PIN -> yangi PIN -> tasdiqlash. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePinScreen(
    busy: Boolean,
    error: String?,
    shake: Int,
    lockRemainingSec: Int,
    onBack: () -> Unit,
    onCheckCurrent: (String, (Boolean) -> Unit) -> Unit,
    onChange: (old: String, new: String) -> Unit,
) {
    var stage by remember { mutableIntStateOf(0) }
    var old by remember { mutableStateOf("") }
    var first by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var localShake by remember { mutableIntStateOf(0) }

    val locked = lockRemainingSec > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PINni o'zgartirish") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                    }
                },
            )
        },
    ) { padding ->
        val title = when (stage) {
            0 -> "Joriy PINni kiriting"
            1 -> "Yangi PIN"
            else -> "Yangi PINni tasdiqlang"
        }
        val subtitle = when {
            stage == 0 && locked -> "Qayta urinish uchun ${formatMmSs(lockRemainingSec)} kuting"
            stage == 0 -> "Xavfsizlik uchun avval joriy PINni tasdiqlang"
            stage == 1 -> "Yangi 6 xonali PIN tanlang"
            else -> "Xuddi shu PINni qayta kiriting"
        }
        PinEntry(
            title = title,
            subtitle = subtitle,
            error = localError ?: error,
            shake = localShake + shake,
            enabled = !busy && !(stage == 0 && locked),
            resetKey = stage,
            modifier = Modifier.padding(padding),
            onComplete = { pin ->
                localError = null
                when (stage) {
                    0 -> onCheckCurrent(pin) { ok ->
                        if (ok) {
                            old = pin
                            stage = 1
                        }
                    }
                    1 -> when {
                        isWeakPin(pin) -> {
                            localError = "Juda oddiy PIN. Boshqasini tanlang."
                            localShake++
                        }
                        pin == old -> {
                            localError = "Yangi PIN joriy PINdan farq qilishi kerak."
                            localShake++
                        }
                        else -> {
                            first = pin
                            stage = 2
                        }
                    }
                    else -> if (pin == first) {
                        onChange(old, pin)
                    } else {
                        localError = "PINlar mos kelmadi. Qaytadan urinib ko'ring."
                        localShake++
                        first = ""
                        stage = 1
                    }
                }
            },
        )
    }
}
