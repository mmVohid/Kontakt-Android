package uz.seyf.kontakt.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uz.seyf.kontakt.data.Contact

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactListScreen(
    contacts: List<Contact>,
    query: String,
    snackbar: SnackbarHostState,
    loadPhoto: PhotoLoader,
    onQuery: (String) -> Unit,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    onLock: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val filtered = remember(contacts, query) { contacts.filter { it.matches(query) } }
    val groups = remember(filtered) { filtered.groupBy { it.letter() } }
    val letterIndex = remember(groups) {
        var i = 0
        val m = LinkedHashMap<String, Int>()
        groups.forEach { (l, g) ->
            m[l] = i
            i += 1 + g.size
        }
        m
    }
    val showScroller = query.isBlank() && filtered.size >= 12 && letterIndex.size >= 3

    Box(Modifier.fillMaxSize().background(softBackdrop())) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = { Text("Kontaktlar", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = onLock) {
                            Icon(Icons.Default.Lock, contentDescription = "Hozir bloklash")
                        }
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Sozlamalar")
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("Yangi kontakt") },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                )
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (contacts.isNotEmpty()) {
                    SearchField(query, "${contacts.size} ta kontakt ichidan qidirish", onQuery)
                }
                Box(Modifier.fillMaxSize()) {
                    when {
                        contacts.isEmpty() -> EmptyState(
                            title = "Hali kontakt yo'q",
                            text = "Birinchi tanishingizni qo'shing. Hamma ma'lumot shifrlangan holda faqat shu telefonda saqlanadi.",
                            action = "Kontakt qo'shish",
                            onAction = onAdd,
                        )
                        filtered.isEmpty() -> EmptyState(
                            title = "Hech narsa topilmadi",
                            text = "Ism, telefon, pasport, JSHSHIR yoki avtomobil raqami bo'yicha qidirib ko'ring.",
                        )
                        else -> LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
                        ) {
                            groups.forEach { (letter, group) ->
                                stickyHeader(key = "h_$letter") { LetterHeader(letter) }
                                items(group, key = { it.id }) { c ->
                                    ContactCard(
                                        contact = c,
                                        query = query,
                                        loadPhoto = loadPhoto,
                                        endPadding = if (showScroller) 32.dp else 16.dp,
                                        modifier = Modifier.animateItem(),
                                        onClick = { onOpen(c.id) },
                                    )
                                }
                            }
                        }
                    }
                    if (showScroller) {
                        AlphabetScroller(
                            letters = letterIndex.keys.toList(),
                            onSelect = { letter ->
                                letterIndex[letter]?.let { idx -> scope.launch { listState.scrollToItem(idx) } }
                            },
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactCard(
    contact: Contact,
    query: String,
    loadPhoto: PhotoLoader,
    endPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val extra = if (query.isBlank()) null else contact.matchedExtra(query)
    val extraValue = highlighted((extra?.second ?: "").take(60), query)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = endPadding, top = 4.dp, bottom = 4.dp),
    ) {
        ListItem(
            leadingContent = { Avatar(contact, 52.dp, loadPhoto) },
            headlineContent = {
                Text(
                    highlighted(contact.displayName, query),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            supportingContent = {
                Column {
                    if (contact.phone.isNotBlank()) {
                        Text(highlighted(contact.phone, query), maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (extra != null) {
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                        ) {
                            Text(
                                text = androidx.compose.ui.text.buildAnnotatedString {
                                    append(extra.first + ": ")
                                    append(extraValue)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Composable
private fun SearchField(query: String, placeholder: String, onQuery: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        TextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Tozalash")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LetterHeader(letter: String) {
    Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f), modifier = Modifier.fillMaxWidth()) {
        Text(
            letter,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp),
        )
    }
}

/** O'ng chetdagi A–Z chizig'i: bosib yoki sudrab kerakli harfga sakrash. */
@Composable
private fun AlphabetScroller(letters: List<String>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var active by remember { mutableStateOf<String?>(null) }
    val haptics = LocalHapticFeedback.current

    Box(modifier) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.8f)
                .width(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f))
                .pointerInput(letters) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun pick(y: Float) {
                            val h = size.height.toFloat()
                            val idx = ((y / h) * letters.size).toInt().coerceIn(0, letters.lastIndex)
                            val l = letters[idx]
                            if (l != active) {
                                active = l
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(l)
                            }
                        }
                        pick(down.position.y)
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull()
                            if (change != null) {
                                pick(change.position.y)
                                change.consume()
                            }
                        } while (event.changes.any { it.pressed })
                        active = null
                    }
                },
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            letters.forEach { l ->
                Text(
                    l,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (l == active) FontWeight.Bold else FontWeight.Medium,
                    color = if (l == active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        active?.let { l ->
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 6.dp,
                modifier = Modifier.align(Alignment.CenterStart).offset(x = (-72).dp).size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(l, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    text: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 40.dp).padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(132.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            )
            Box(
                Modifier.size(92.dp).clip(CircleShape).background(brandBrush()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(42.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}
