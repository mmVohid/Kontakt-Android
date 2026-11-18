package uz.seyf.kontakt.ui

import androidx.compose.foundation.clickable
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    working: Boolean,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onBioToggle: (Boolean) -> Unit,
    onChangePin: () -> Unit,
    onIdleTimeout: (Int) -> Unit,
    onLockNow: () -> Unit,
    setExternalFlow: (Boolean) -> Unit,
    onExport: (Uri, String) -> Unit,
    onImport: (Uri, String) -> Unit,
) {
    var askIdle by remember { mutableStateOf(false) }
    var askExportPassword by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    val transparent = ListItemDefaults.colors(containerColor = Color.Transparent)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        setExternalFlow(false)
        val pwd = exportPassword
        exportPassword = ""
        if (uri != null && pwd.isNotEmpty()) onExport(uri, pwd)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        setExternalFlow(false)
        if (uri != null) importUri = uri
    }

    Box(Modifier.fillMaxSize().background(softBackdrop())) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    title = { Text("Sozlamalar", fontWeight = FontWeight.Bold) },
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
                if (working) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 8.dp))
                }

                SectionTitle("Kirish")
                Spacer(Modifier.height(8.dp))
                SoftCard {
                    ListItem(
                        colors = transparent,
                        leadingContent = { IconBubble(Icons.Default.Fingerprint) },
                        headlineContent = { Text("Biometrik kirish") },
                        supportingContent = {
                            Text(if (bioAvailable) "Barmoq izi yoki yuz bilan tez ochish" else "Qurilmada biometrik ma'lumot sozlanmagan")
                        },
                        trailingContent = {
                            Switch(checked = bioEnabled, onCheckedChange = onBioToggle, enabled = bioAvailable)
                        },
                    )
                    HorizontalDivider(Modifier.padding(start = 72.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    ListItem(
                        modifier = Modifier.clickable(onClick = onChangePin),
                        colors = transparent,
                        leadingContent = { IconBubble(Icons.Default.Pin) },
                        headlineContent = { Text("PINni o'zgartirish") },
                        supportingContent = { Text("6 xonali kirish kodi") },
                    )
                    HorizontalDivider(Modifier.padding(start = 72.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    ListItem(
                        modifier = Modifier.clickable { askIdle = true },
                        colors = transparent,
                        leadingContent = { IconBubble(Icons.Default.Timer) },
                        headlineContent = { Text("Harakatsizlikda bloklash") },
                        supportingContent = {
                            Text(IdleOptions.firstOrNull { it.first == idleTimeoutSec }?.second ?: "$idleTimeoutSec soniya")
                        },
                    )
                }

                Spacer(Modifier.height(24.dp))
                SectionTitle("Zaxira nusxa")
                Spacer(Modifier.height(8.dp))
                SoftCard {
                    ListItem(
                        modifier = Modifier.clickable(enabled = !working) { askExportPassword = true },
                        colors = transparent,
                        leadingContent = { IconBubble(Icons.Default.Upload) },
                        headlineContent = { Text("Zaxira nusxa yaratish") },
                        supportingContent = { Text("Parol bilan shifrlangan fayl sifatida saqlash") },
                    )
                    HorizontalDivider(Modifier.padding(start = 72.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    ListItem(
                        modifier = Modifier.clickable(enabled = !working) {
                            setExternalFlow(true)
                            importLauncher.launch(arrayOf("*/*"))
                        },
                        colors = transparent,
                        leadingContent = { IconBubble(Icons.Default.Download) },
                        headlineContent = { Text("Zaxiradan tiklash") },
                        supportingContent = { Text("Mavjud kontaktlarga qo'shiladi, hech narsa o'chmaydi") },
                    )
                }

                Spacer(Modifier.height(24.dp))
                SectionTitle("Himoya holati")
                Spacer(Modifier.height(8.dp))
                SoftCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                FilledTonalButton(
                    onClick = onLockNow,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Hozir bloklash")
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Seyf Kontakt 1.1.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(32.dp))
            }
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

    if (askExportPassword) {
        PasswordDialog(
            title = "Zaxira paroli",
            message = "Zaxira fayli shu parol bilan shifrlanadi. Parolni unutsangiz, faylni ochib bo'lmaydi. Uni PINdan farqli, esda qoladigan qilib tanlang (kamida 8 belgi).",
            confirmLabel = "Davom etish",
            requireConfirm = true,
            onDismiss = { askExportPassword = false },
            onConfirm = { pwd ->
                askExportPassword = false
                exportPassword = pwd
                setExternalFlow(true)
                val date = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                exportLauncher.launch("seyf-kontakt-$date.seyfbak")
            },
        )
    }

    importUri?.let { uri ->
        PasswordDialog(
            title = "Zaxira parolini kiriting",
            message = "Zaxira yaratishda tanlagan parolingizni kiriting.",
            confirmLabel = "Tiklash",
            requireConfirm = false,
            onDismiss = { importUri = null },
            onConfirm = { pwd ->
                importUri = null
                onImport(uri, pwd)
            },
        )
    }
}

@Composable
private fun SoftCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column { content() }
    }
}

@Composable
private fun PasswordDialog(
    title: String,
    message: String,
    confirmLabel: String,
    requireConfirm: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pwd by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    val tooShort = requireConfirm && pwd.isNotEmpty() && pwd.length < 8
    val mismatch = requireConfirm && again.isNotEmpty() && again != pwd
    val valid = pwd.isNotEmpty() && (!requireConfirm || (pwd.length >= 8 && pwd == again))
    val transformation = if (show) VisualTransformation.None else PasswordVisualTransformation()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it.take(64) },
                    label = { Text("Parol") },
                    singleLine = true,
                    isError = tooShort,
                    supportingText = if (tooShort) ({ Text("Kamida 8 belgi") }) else null,
                    visualTransformation = transformation,
                    trailingIcon = {
                        IconButton(onClick = { show = !show }) {
                            Icon(
                                if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (show) "Yashirish" else "Ko'rsatish",
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (requireConfirm) ImeAction.Next else ImeAction.Done,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (requireConfirm) {
                    OutlinedTextField(
                        value = again,
                        onValueChange = { again = it.take(64) },
                        label = { Text("Parolni takrorlang") },
                        singleLine = true,
                        isError = mismatch,
                        supportingText = if (mismatch) ({ Text("Parollar mos kelmadi") }) else null,
                        visualTransformation = transformation,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(pwd) }, enabled = valid) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Bekor qilish") } },
    )
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
