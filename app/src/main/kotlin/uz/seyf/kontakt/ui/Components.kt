package uz.seyf.kontakt.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import uz.seyf.kontakt.data.Contact
import kotlin.math.abs

private val AvatarColors = listOf(
    Color(0xFF3949AB), Color(0xFF00796B), Color(0xFFC25B00), Color(0xFF8E24AA),
    Color(0xFFAD1457), Color(0xFF00695C), Color(0xFF6D4C41), Color(0xFF1565C0),
)

typealias PhotoLoader = suspend (String, Int) -> ImageBitmap?

/**
 * Doira shaklidagi avatar. Rasm bo'lsa — rasm, bo'lmasa — ism bosh harflari.
 * [override] berilsa (masalan, endi tanlangan rasm) — shu ko'rsatiladi.
 */
@Composable
fun Avatar(
    contact: Contact?,
    size: Dp,
    loadPhoto: PhotoLoader,
    modifier: Modifier = Modifier,
    override: ImageBitmap? = null,
) {
    val stored by produceState<ImageBitmap?>(null, contact?.id, contact?.hasPhoto, contact?.updatedAt) {
        value = if (contact != null && contact.hasPhoto) {
            loadPhoto(contact.id, if (size > 80.dp) 512 else 160)
        } else null
    }
    val shown = override ?: stored
    if (shown != null) {
        Image(
            bitmap = shown,
            contentDescription = contact?.displayName,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape),
        )
    } else {
        val bg = if (contact == null) MaterialTheme.colorScheme.surfaceContainerHighest
        else AvatarColors[abs(contact.id.hashCode()) % AvatarColors.size]
        Box(
            modifier = modifier.size(size).clip(CircleShape).background(bg),
            contentAlignment = Alignment.Center,
        ) {
            val initials = contact?.initials.orEmpty()
            if (initials.isNotEmpty()) {
                Text(
                    initials,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (size.value * 0.38f).sp,
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = if (contact == null) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                    modifier = Modifier.size(size * 0.5f),
                )
            }
        }
    }
}

/** Ma'lumot qatori: belgi, sarlavha, qiymat, (maxfiy bo'lsa) ko'rsatish tugmasi va nusxalash. */
@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    sensitive: Boolean = false,
) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(revealed) {
        if (revealed) {
            delay(15_000)
            revealed = false
        }
    }
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = if (sensitive && !revealed) "•".repeat(value.length.coerceIn(4, 14)) else value,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (sensitive) {
            IconButton(onClick = { revealed = !revealed }) {
                Icon(
                    if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (revealed) "Yashirish" else "Ko'rsatish",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onCopy) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Nusxalash: $label",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

@Composable
fun RowSpacer(w: Dp = 8.dp) = Spacer(Modifier.width(w))

@Composable
fun CenteredRow(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { content() }
}
