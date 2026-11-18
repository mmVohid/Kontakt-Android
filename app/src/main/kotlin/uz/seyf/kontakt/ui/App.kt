package uz.seyf.kontakt.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.seyf.kontakt.AppViewModel
import uz.seyf.kontakt.Phase
import uz.seyf.kontakt.Route
import uz.seyf.kontakt.UiState

@Composable
fun SeyfApp(
    vm: AppViewModel,
    onBioUnlock: () -> Unit,
    onBioToggle: (Boolean) -> Unit,
) {
    SeyfTheme {
        val s by vm.state.collectAsStateWithLifecycle()
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AnimatedContent(
                targetState = s.phase,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
                label = "phase",
            ) { phase ->
                when (phase) {
                    Phase.Setup, Phase.Locked -> LockScreen(
                        isSetup = phase == Phase.Setup,
                        busy = s.busy,
                        error = s.pinError,
                        shake = s.shake,
                        lockRemainingSec = s.lockRemainingSec,
                        bioShown = s.bioAvailable && s.bioEnabled,
                        onSubmit = vm::tryUnlock,
                        onSetPin = vm::setPin,
                        onBio = onBioUnlock,
                        onWipe = vm::wipeAll,
                    )
                    Phase.Unlocked -> MainNav(vm, s, onBioToggle)
                }
            }
        }
    }
}

@Composable
private fun MainNav(vm: AppViewModel, s: UiState, onBioToggle: (Boolean) -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            val result = snackbar.showSnackbar(
                message = e.message,
                actionLabel = e.actionLabel,
                duration = if (e.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) e.onAction?.invoke()
        }
    }

    BackHandler(enabled = s.route != Route.Home) { vm.back() }

    val loadPhoto: PhotoLoader = { id, px -> vm.photo(id, px) }

    AnimatedContent(
        targetState = s.route,
        transitionSpec = {
            (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 14 }) togetherWith fadeOut(tween(120))
        },
        label = "route",
    ) { route ->
        when (route) {
            Route.Home -> ContactListScreen(
                contacts = s.contacts,
                query = s.query,
                snackbar = snackbar,
                loadPhoto = loadPhoto,
                onQuery = vm::setQuery,
                onOpen = vm::openContact,
                onAdd = vm::newContact,
                onSettings = vm::openSettings,
                onLock = vm::lock,
            )
            is Route.Detail -> ContactDetailScreen(
                contact = s.contacts.find { it.id == route.id },
                snackbar = snackbar,
                loadPhoto = loadPhoto,
                onBack = vm::back,
                onEdit = { vm.editContact(route.id) },
                onDelete = { vm.deleteContact(route.id) },
                onCopy = { text, label -> vm.copy(text, label) },
            )
            is Route.Edit -> ContactEditScreen(
                existing = route.id?.let { id -> s.contacts.find { it.id == id } },
                snackbar = snackbar,
                loadPhoto = loadPhoto,
                processPhoto = { uri -> vm.processPhoto(uri) },
                prepareCapture = { vm.prepareCapture() },
                processCapture = { ok -> vm.processCapture(ok) },
                setExternalFlow = { vm.externalFlow = it },
                onActivity = vm::poke,
                onToast = vm::toast,
                onSave = { contact, photo, remove -> vm.saveContact(contact, photo, remove) },
                onBack = vm::back,
            )
            Route.Settings -> SettingsScreen(
                bioAvailable = s.bioAvailable,
                bioEnabled = s.bioEnabled,
                idleTimeoutSec = s.idleTimeoutSec,
                working = s.working,
                snackbar = snackbar,
                onBack = vm::back,
                onBioToggle = onBioToggle,
                onChangePin = vm::openChangePin,
                onIdleTimeout = vm::setIdleTimeout,
                onLockNow = vm::lock,
                setExternalFlow = { vm.externalFlow = it },
                onExport = { uri, pwd -> vm.exportBackup(uri, pwd) },
                onImport = { uri, pwd -> vm.importBackup(uri, pwd) },
            )
            Route.ChangePin -> ChangePinScreen(
                busy = s.busy,
                error = s.pinError,
                shake = s.shake,
                lockRemainingSec = s.lockRemainingSec,
                onBack = vm::back,
                onCheckCurrent = { pin, cb -> vm.checkCurrentPin(pin, cb) },
                onChange = { old, new -> vm.changePin(old, new) },
            )
        }
    }

    if (s.offerBio) {
        AlertDialog(
            onDismissRequest = vm::dismissBioOffer,
            title = { Text("Biometrik kirishni yoqamizmi?") },
            text = { Text("Keyingi safar PIN o'rniga barmoq izi yoki yuz bilan tez ochishingiz mumkin. PIN baribir zaxira sifatida qoladi.") },
            confirmButton = {
                TextButton(onClick = { vm.dismissBioOffer(); onBioToggle(true) }) { Text("Yoqish") }
            },
            dismissButton = { TextButton(onClick = vm::dismissBioOffer) { Text("Keyinroq") } },
        )
    }
}
