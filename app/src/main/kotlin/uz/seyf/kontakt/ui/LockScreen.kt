package uz.seyf.kontakt.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

const val PIN_LENGTH = 6

fun isWeakPin(p: String): Boolean {
    if (p.length != PIN_LENGTH) return true
    if (p.all { it == p[0] }) return true
    if ("0123456789".contains(p) || "9876543210".contains(p)) return true
    if (p == p.take(2).repeat(3) || p == p.take(3).repeat(2)) return true
    return false
}

fun formatMmSs(sec: Int): String = "%02d:%02d".format(sec / 60, sec % 60)

/** PIN kiritish: nuqtalar + raqamli klaviatura. Tizim klaviaturasi ishlatilmaydi (klaviatura yozuvlari oqib ketmasligi uchun). */
@Composable
fun PinEntry(
    title: String,
    subtitle: String,
    error: String?,
    shake: Int,
    enabled: Boolean,
    resetKey: Any,
    onComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    leftSlot: (@Composable () -> Unit)? = null,
) {
    var pin by remember(resetKey) { mutableStateOf("") }
    val haptics = LocalHapticFeedback.current
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(shake) {
        if (shake > 0) {
            pin = ""
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            repeat(3) {
                offsetX.animateTo(14f, tween(45))
                offsetX.animateTo(-14f, tween(45))
            }
            offsetX.animateTo(0f, tween(45))
        }
    }
    LaunchedEffect(pin) {
        if (pin.length == PIN_LENGTH) onComplete(pin)
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val compact = maxHeight < 620.dp
        val keySize = if (compact) 60.dp else 74.dp

        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(if (compact) 52.dp else 68.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(if (compact) 26.dp else 32.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(if (compact) 18.dp else 28.dp))

            Row(
                Modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .semantics { contentDescription = "PIN: ${pin.length} / $PIN_LENGTH raqam kiritildi" },
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                repeat(PIN_LENGTH) { i ->
                    val filled = i < pin.length
                    val color by animateColorAsState(
                        if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        label = "dot",
                    )
                    Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(if (filled) 16.dp else 12.dp).clip(CircleShape).background(color))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.height(44.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    error.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.weight(1f))

            fun add(d: String) {
                if (enabled && pin.length < PIN_LENGTH) {
                    pin += d
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)) {
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        row.forEach { d -> KeyButton(d, keySize, enabled) { add(d) } }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(Modifier.size(keySize), contentAlignment = Alignment.Center) { leftSlot?.invoke() }
                    KeyButton("0", keySize, enabled) { add("0") }
                    Box(Modifier.size(keySize), contentAlignment = Alignment.Center) {
                        if (pin.isNotEmpty()) {
                            IconButton(onClick = { pin = pin.dropLast(1) }, enabled = enabled) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Oxirgi raqamni o'chirish",
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun KeyButton(label: String, size: Dp, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.4f)
            .semantics { contentDescription = "Raqam $label" },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Birinchi ishga tushirish (PIN o'rnatish) va bloklangan holat (PIN / biometrika bilan ochish). */
@Composable
fun LockScreen(
    isSetup: Boolean,
    busy: Boolean,
    error: String?,
    shake: Int,
    lockRemainingSec: Int,
    bioShown: Boolean,
    onSubmit: (String) -> Unit,
    onSetPin: (String) -> Unit,
    onBio: () -> Unit,
    onWipe: () -> Unit,
) {
    var stage by remember { mutableIntStateOf(0) }
    var first by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var localShake by remember { mutableIntStateOf(0) }
    var askWipe by remember { mutableStateOf(false) }

    val locked = lockRemainingSec > 0

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        if (!isSetup) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { askWipe = true }) { Text("PINni unutdim") }
            }
        } else {
            Spacer(Modifier.height(48.dp))
        }

        val title: String
        val subtitle: String
        when {
            isSetup && stage == 0 -> {
                title = "PIN o'rnating"
                subtitle = "Kontaktlaringizni himoya qilish uchun 6 xonali PIN tanlang.\nUni unutsangiz, ma'lumotni tiklab bo'lmaydi."
            }
            isSetup -> {
                title = "PINni tasdiqlang"
                subtitle = "Xuddi shu PINni qayta kiriting"
            }
            locked -> {
                title = "Vaqtincha bloklangan"
                subtitle = "Qayta urinish uchun ${formatMmSs(lockRemainingSec)} kuting"
            }
            else -> {
                title = "Seyf Kontakt"
                subtitle = "PIN kiriting"
            }
        }

        PinEntry(
            title = title,
            subtitle = subtitle,
            error = localError ?: error,
            shake = localShake + shake,
            enabled = !busy && !locked,
            resetKey = stage,
            modifier = Modifier.weight(1f),
            leftSlot = if (!isSetup && bioShown) {
                {
                    IconButton(onClick = onBio, enabled = !busy && !locked) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = "Biometrik kirish",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            } else null,
            onComplete = { pin ->
                localError = null
                if (!isSetup) {
                    onSubmit(pin)
                } else if (stage == 0) {
                    if (isWeakPin(pin)) {
                        localError = "Juda oddiy PIN (masalan, 123456 yoki 111111). Boshqasini tanlang."
                        localShake++
                    } else {
                        first = pin
                        stage = 1
                    }
                } else {
                    if (pin == first) {
                        onSetPin(pin)
                    } else {
                        localError = "PINlar mos kelmadi. Qaytadan urinib ko'ring."
                        localShake++
                        first = ""
                        stage = 0
                    }
                }
            },
        )
    }

    if (askWipe) {
        AlertDialog(
            onDismissRequest = { askWipe = false },
            title = { Text("Seyfni tozalash?") },
            text = {
                Text(
                    "PINni tiklab bo'lmaydi. Davom etsangiz, barcha kontaktlar va rasmlar butunlay " +
                        "o'chiriladi va yangi PIN o'rnatishingiz kerak bo'ladi. Bu amalni qaytarib bo'lmaydi."
                )
            },
            confirmButton = {
                TextButton(onClick = { askWipe = false; onWipe() }) {
                    Text("Hammasini o'chirish", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { askWipe = false }) { Text("Bekor qilish") } },
        )
    }
}
