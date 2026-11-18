package uz.seyf.kontakt.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uz.seyf.kontakt.data.Contact
import uz.seyf.kontakt.data.PhotoUtils
import java.util.UUID

@Stable
class EditState(c: Contact?) {
    var firstName by mutableStateOf(c?.firstName.orEmpty())
    var lastName by mutableStateOf(c?.lastName.orEmpty())
    var patronymic by mutableStateOf(c?.patronymic.orEmpty())
    var phone by mutableStateOf(c?.phone.orEmpty())
    var passport by mutableStateOf(c?.passportSeries.orEmpty())
    var pinfl by mutableStateOf(c?.pinfl.orEmpty())
    var address by mutableStateOf(c?.address.orEmpty())
    var plate by mutableStateOf(c?.carPlate.orEmpty())
    var note by mutableStateOf(c?.note.orEmpty())
    var newPhoto by mutableStateOf<ByteArray?>(null)
    var removePhoto by mutableStateOf(false)
    var showErrors by mutableStateOf(false)

    private val initial = snapshot()

    private fun snapshot() = listOf(firstName, lastName, patronymic, phone, passport, pinfl, address, plate, note)

    val dirty: Boolean get() = newPhoto != null || removePhoto || snapshot() != initial

    val nameError get() = firstName.isBlank()
    val phoneError get() = phone.isNotBlank() && phone.count { it.isDigit() } < 7
    val passportError get() = passport.isNotEmpty() && !PASSPORT_REGEX.matches(passport)
    val pinflError get() = pinfl.isNotEmpty() && pinfl.length != 14
    val hasErrors get() = nameError || phoneError || passportError || pinflError

    private companion object {
        val PASSPORT_REGEX = Regex("^[A-Z]{2}\\d{7}$")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactEditScreen(
    existing: Contact?,
    snackbar: SnackbarHostState,
    loadPhoto: PhotoLoader,
    processPhoto: suspend (android.net.Uri) -> ByteArray?,
    setExternalFlow: (Boolean) -> Unit,
    onActivity: () -> Unit,
    onSave: (Contact, ByteArray?, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val s = remember(existing?.id) { EditState(existing) }
    val scope = rememberCoroutineScope()
    var askDiscard by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    val requestBack: () -> Unit = {
        if (s.dirty) {
            askDiscard = true
        } else {
            onBack()
        }
    }
    BackHandler(onBack = requestBack)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        setExternalFlow(false)
        if (uri != null) {
            scope.launch {
                val bytes = processPhoto(uri)
                if (bytes != null) {
                    s.newPhoto = bytes
                    s.removePhoto = false
                    onActivity()
                }
            }
        }
    }

    val newBitmap = remember(s.newPhoto) { s.newPhoto?.let { PhotoUtils.decodeSampled(it, 512) } }
    val hasAnyPhoto = newBitmap != null || (existing?.hasPhoto == true && !s.removePhoto)

    val preview = Contact(
        id = existing?.id ?: "preview-new",
        firstName = s.firstName, lastName = s.lastName,
        hasPhoto = existing?.hasPhoto == true && !s.removePhoto && s.newPhoto == null,
        updatedAt = existing?.updatedAt ?: 0L,
    )

    fun save() {
        if (s.hasErrors) {
            s.showErrors = true
            return
        }
        val contact = Contact(
            id = existing?.id ?: UUID.randomUUID().toString(),
            firstName = s.firstName.trim(),
            lastName = s.lastName.trim(),
            patronymic = s.patronymic.trim(),
            phone = s.phone.trim(),
            passportSeries = s.passport.trim(),
            pinfl = s.pinfl.trim(),
            address = s.address.trim(),
            carPlate = s.plate.trim(),
            note = s.note.trim(),
        )
        onSave(contact, s.newPhoto, s.removePhoto)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Yangi kontakt" else "Tahrirlash") },
                navigationIcon = {
                    IconButton(onClick = requestBack) { Icon(Icons.Default.Close, contentDescription = "Yopish") }
                },
                actions = { TextButton(onClick = { save() }) { Text("Saqlash", fontWeight = FontWeight.SemiBold) } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.clickable {
                    setExternalFlow(true)
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            ) {
                Avatar(preview, 112.dp, loadPhoto, override = newBitmap)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.BottomEnd).size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Rasm tanlash",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = {
                    setExternalFlow(true)
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(if (hasAnyPhoto) "Rasmni almashtirish" else "Rasm qo'shish") }
                if (hasAnyPhoto) {
                    TextButton(onClick = {
                        s.newPhoto = null
                        s.removePhoto = true
                        onActivity()
                    }) { Text("Olib tashlash", color = MaterialTheme.colorScheme.error) }
                }
            }
            Spacer(Modifier.height(8.dp))

            FormCard("Asosiy ma'lumot") {
                Field(
                    s.firstName, { s.firstName = it.take(40); onActivity() }, "Ism", Icons.Default.Person,
                    capitalization = KeyboardCapitalization.Words,
                    isError = s.showErrors && s.nameError,
                    supporting = if (s.showErrors && s.nameError) "Ism kiritilishi shart" else null,
                )
                Field(
                    s.lastName, { s.lastName = it.take(40); onActivity() }, "Familya", Icons.Default.Person,
                    capitalization = KeyboardCapitalization.Words,
                )
                Field(
                    s.patronymic, { s.patronymic = it.take(40); onActivity() }, "Otasining ismi", Icons.Default.Person,
                    capitalization = KeyboardCapitalization.Words,
                )
            }

            FormCard("Aloqa") {
                Field(
                    s.phone,
                    { v -> s.phone = v.filter { it.isDigit() || it in "+ -()" }.take(20); onActivity() },
                    "Telefon raqami", Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone,
                    placeholder = "+998 90 123 45 67",
                    isError = s.showErrors && s.phoneError,
                    supporting = if (s.showErrors && s.phoneError) "Telefon raqami to'liq emas" else null,
                )
            }

            FormCard("Hujjatlar") {
                Field(
                    s.passport,
                    { v -> s.passport = v.uppercase().filter { it.isLetterOrDigit() }.take(9); onActivity() },
                    "Pasport seriyasi", Icons.Default.Badge,
                    keyboardType = KeyboardType.Password,
                    capitalization = KeyboardCapitalization.Characters,
                    placeholder = "AA1234567",
                    isError = s.passport.isNotEmpty() && (s.showErrors || s.passport.length == 9) && s.passportError,
                    supporting = if (s.passport.isNotEmpty() && (s.showErrors || s.passport.length == 9) && s.passportError)
                        "Format: 2 harf + 7 raqam (AA1234567)" else "Format: AA1234567",
                )
                Field(
                    s.pinfl,
                    { v -> s.pinfl = v.filter { it.isDigit() }.take(14); onActivity() },
                    "Pasport JSHSHIR (PINFL)", Icons.Default.Fingerprint,
                    keyboardType = KeyboardType.NumberPassword,
                    isError = s.showErrors && s.pinflError,
                    supporting = if (s.showErrors && s.pinflError) "JSHSHIR 14 ta raqamdan iborat (${s.pinfl.length}/14)"
                    else "${s.pinfl.length}/14",
                )
            }

            FormCard("Qo'shimcha") {
                Field(
                    s.address, { s.address = it.take(200); onActivity() }, "Yashash joyi", Icons.Default.Home,
                    capitalization = KeyboardCapitalization.Sentences,
                    singleLine = false, minLines = 2,
                )
                Field(
                    s.plate,
                    { v -> s.plate = v.uppercase().filter { it.isLetterOrDigit() || it == ' ' }.take(12); onActivity() },
                    "Avtomobil raqami", Icons.Default.DirectionsCar,
                    keyboardType = KeyboardType.Password,
                    capitalization = KeyboardCapitalization.Characters,
                    placeholder = "01 A 123 BC",
                )
                Field(
                    s.note, { s.note = it.take(1000); onActivity() }, "Izoh", Icons.AutoMirrored.Filled.Notes,
                    capitalization = KeyboardCapitalization.Sentences,
                    singleLine = false, minLines = 3, imeAction = ImeAction.Default,
                )
            }

            Spacer(Modifier.height(8.dp))
            Button(onClick = { save() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Saqlash", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (askDiscard) {
        AlertDialog(
            onDismissRequest = { askDiscard = false },
            title = { Text("O'zgarishlar saqlanmasin?") },
            text = { Text("Kiritilgan ma'lumotlar yo'qoladi.") },
            confirmButton = {
                TextButton(onClick = { askDiscard = false; onBack() }) {
                    Text("Chiqish", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { askDiscard = false }) { Text("Davom etish") } },
        )
    }
}

@Composable
private fun FormCard(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(8.dp))
    Column(Modifier.fillMaxWidth()) {
        SectionTitle(title)
        Spacer(Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    placeholder: String? = null,
    isError: Boolean = false,
    supporting: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = { Icon(icon, contentDescription = null) },
        supportingText = supporting?.let { { Text(it) } },
        isError = isError,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}
