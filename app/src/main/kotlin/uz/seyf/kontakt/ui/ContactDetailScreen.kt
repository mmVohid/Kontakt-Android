package uz.seyf.kontakt.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uz.seyf.kontakt.data.Contact
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contact: Contact?,
    snackbar: SnackbarHostState,
    loadPhoto: PhotoLoader,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: (text: String, label: String) -> Unit,
) {
    if (contact == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val ctx = LocalContext.current
    var askDelete by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(softBackdrop())) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Tahrirlash") }
                    IconButton(onClick = { askDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "O'chirish")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(146.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                )
                Avatar(contact, 124.dp, loadPhoto)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                contact.displayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (contact.phone.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    contact.phone,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    QuickAction(Icons.Default.Call, "Qo'ng'iroq") { ctx.startSafely(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(dialable(contact.phone))))) }
                    QuickAction(Icons.AutoMirrored.Filled.Message, "SMS") { ctx.startSafely(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(dialable(contact.phone))))) }
                    QuickAction(Icons.Default.ContentCopy, "Nusxa") { onCopy(contact.phone, "Telefon") }
                }
            }
            Spacer(Modifier.height(24.dp))

            val rows = buildList<@Composable () -> Unit> {
                if (contact.phone.isNotBlank()) add {
                    InfoRow(Icons.Default.Phone, "Telefon raqami", contact.phone, { onCopy(contact.phone, "Telefon") })
                }
                if (contact.lastName.isNotBlank()) add {
                    InfoRow(Icons.Default.Person, "Familya", contact.lastName, { onCopy(contact.lastName, "Familya") })
                }
                if (contact.patronymic.isNotBlank()) add {
                    InfoRow(Icons.Default.Person, "Otasining ismi", contact.patronymic, { onCopy(contact.patronymic, "Otasining ismi") })
                }
                if (contact.passportSeries.isNotBlank()) add {
                    InfoRow(Icons.Default.Badge, "Pasport seriyasi", contact.passportSeries, { onCopy(contact.passportSeries, "Pasport seriyasi") }, sensitive = true)
                }
                if (contact.pinfl.isNotBlank()) add {
                    InfoRow(Icons.Default.Fingerprint, "Pasport JSHSHIR", contact.pinfl, { onCopy(contact.pinfl, "JSHSHIR") }, sensitive = true)
                }
                if (contact.address.isNotBlank()) add {
                    InfoRow(Icons.Default.Home, "Yashash joyi", contact.address, { onCopy(contact.address, "Manzil") })
                }
                if (contact.carPlate.isNotBlank()) add {
                    InfoRow(Icons.Default.DirectionsCar, "Avtomobil raqami", contact.carPlate, { onCopy(contact.carPlate, "Avtomobil raqami") })
                }
                if (contact.note.isNotBlank()) add {
                    InfoRow(Icons.AutoMirrored.Filled.Notes, "Izoh", contact.note, { onCopy(contact.note, "Izoh") })
                }
            }

            if (rows.isEmpty()) {
                Text(
                    "Qo'shimcha ma'lumot kiritilmagan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    rows.forEachIndexed { i, row ->
                        if (i > 0) HorizontalDivider(Modifier.padding(start = 70.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        row()
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            val fmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
            Text(
                "Yangilangan: ${fmt.format(Date(contact.updatedAt))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
    }

    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            title = { Text("Kontaktni o'chirish?") },
            text = { Text("${contact.displayName} butunlay o'chiriladi. Bu amalni qaytarib bo'lmaydi.") },
            confirmButton = {
                TextButton(onClick = { askDelete = false; onDelete() }) {
                    Text("O'chirish", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { askDelete = false }) { Text("Bekor qilish") } },
        )
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(60.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(icon, contentDescription = label)
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun dialable(phone: String): String = phone.filter { it.isDigit() || it == '+' }

private fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}
