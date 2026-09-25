package com.example.ui.components

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.viewmodel.DriveBackupUiState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope

@Composable
fun DriveBackupSection(
    state: DriveBackupUiState,
    onConnect: (GoogleSignInAccount) -> Unit,
    onDisconnect: () -> Unit,
    onAutomaticChanged: (Boolean) -> Unit,
    onBackupNow: (GoogleSignInAccount, String) -> Unit,
    onRestore: (GoogleSignInAccount, String) -> Unit
) {
    val context = LocalContext.current
    var account by remember { mutableStateOf(GoogleSignIn.getLastSignedInAccount(context)) }
    var passphrase by remember { mutableStateOf("") }
    var showPassphraseDialog by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .addOnSuccessListener { signedIn ->
                    account = signedIn
                    onConnect(signedIn)
                }
        }
    }

    val signInClient = remember {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
                .build()
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.google_drive_backup), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(
                if (state.accountName != null) stringResource(R.string.google_connected_as, state.accountName!!)
                else stringResource(R.string.google_drive_disconnected),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
            )
            state.lastSuccessfulBackup.takeIf { it > 0 }?.let {
                Text(stringResource(R.string.last_backup, java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))))
            }
            state.availableBackup?.let { backup ->
                Text(stringResource(R.string.backup_found, java.text.DateFormat.getDateTimeInstance().format(java.util.Date(backup.createdAt))))
                Text(stringResource(R.string.backup_version, backup.appVersion, backup.formatVersion))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.automatic_backup))
                Switch(checked = state.automaticEnabled, onCheckedChange = onAutomaticChanged, enabled = state.accountName != null)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.accountName == null) {
                    Button(onClick = { signInLauncher.launch(signInClient.signInIntent) }) {
                        Text(stringResource(R.string.connect_google))
                    }
                } else {
                    Button(onClick = { showPassphraseDialog = "backup" }, enabled = !state.isBusy) {
                        Text(stringResource(R.string.back_up_now))
                    }
                    Button(onClick = { showRestoreConfirmation = true }, enabled = !state.isBusy) {
                        Text(stringResource(R.string.restore_backup))
                    }
                    TextButton(onClick = onDisconnect) { Text(stringResource(R.string.disconnect_google)) }
                }
            }
            if (state.isBusy) {
                Text(stringResource(R.string.backup_in_progress))
            }
            state.message?.let { message ->
                Text(
                    when (message) {
                        "success" -> stringResource(R.string.backup_success)
                        "restored" -> stringResource(R.string.restore_success)
                        else -> stringResource(R.string.backup_failed, message)
                    }
                )
            }
        }
    }

    if (showRestoreConfirmation) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmation = false },
            title = { Text(stringResource(R.string.restore_backup)) },
            text = { Text(stringResource(R.string.restore_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirmation = false
                    showPassphraseDialog = "restore"
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = { TextButton(onClick = { showRestoreConfirmation = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    showPassphraseDialog?.let { action ->
        AlertDialog(
            onDismissRequest = { showPassphraseDialog = null },
            title = { Text(stringResource(R.string.backup_passphrase)) },
            text = {
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text(stringResource(R.string.backup_passphrase)) },
                    visualTransformation = PasswordVisualTransformation()
                )
                if (passphrase.isNotEmpty() && passphrase.length < 12) {
                    Text(stringResource(R.string.backup_passphrase_requirement))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val signedIn = account ?: GoogleSignIn.getLastSignedInAccount(context)
                    if (signedIn != null && passphrase.length >= 12) {
                        if (action == "backup") onBackupNow(signedIn, passphrase) else onRestore(signedIn, passphrase)
                        showPassphraseDialog = null
                    }
                }) { Text(stringResource(R.string.continue_action)) }
            },
            dismissButton = { TextButton(onClick = { showPassphraseDialog = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
