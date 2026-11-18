package uz.seyf.kontakt.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import uz.seyf.kontakt.data.Contact
import kotlin.math.abs

private val AvatarGradients = listOf(
    Color(0xFF5C6BC0) to Color(0xFF3949AB),
    Color(0xFF26A69A) to Color(0xFF00796B),
    Color(0xFFFF9A5A) to Color(0xFFE0651A),
    Color(0xFFAB5CC9) to Color(0xFF7B1FA2),
    Color(0xFFEC6A9B) to Color(0xFFC2185B),
    Color(0xFF4DB6AC) to Color(0xFF00897B),
    Color(0xFF9C7B6B) to Color(0xFF6D4C41),
    Color(0xFF5AA9F0) to Color(0xFF1565C0),
)

typealias PhotoLoader = suspend (String, Int) -> ImageBitmap?

/**
 * Doira shaklidagi avatar. Rasm bo'lsa, rasm; bo'lmasa, ism bosh harflari gradient fonda.
 * [override] berilsa (masalan, endi tanlangan rasm), shu ko'rsatiladi.
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
        val brush: Brush = if (contact == null) {
            Brush.linearGradient(
                listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHighest),
            )
        } else {
            val pair = AvatarGradients[abs(contact.id.hashCode()) % AvatarGradients.size]
            Brush.linearGradient(listOf(pair.first, pair.second))
        }
        Box(
            modifier = modifier.size(size).clip(CircleShape).background(brush),
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

/** Yumshoq rangli doira ichidagi belgi. */
@Composable
fun IconBubble(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** [query] topilgan joyni rangli fon bilan ajratib ko'rsatadi. */
@Composable
fun highlighted(text: String, query: String): AnnotatedString {
    val q = query.trim()
    val bg = MaterialTheme.colorScheme.tertiaryContainer
    val fg = MaterialTheme.colorScheme.onTertiaryContainer
    return remember(text, q, bg, fg) {
        buildAnnotatedString {
            append(text)
            if (q.isNotEmpty()) {
                val i = text.indexOf(q, ignoreCase = true)
                if (i >= 0) addStyle(SpanStyle(background = bg, color = fg, fontWeight = FontWeight.SemiBold), i, i + q.length)
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
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(icon)
        Spacer(Modifier.width(14.dp))
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
        modifier = modifier.padding(horizontal = 8.dp),
    )
}
