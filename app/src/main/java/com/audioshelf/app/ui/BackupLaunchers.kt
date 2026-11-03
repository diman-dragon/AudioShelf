package com.audioshelf.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.audioshelf.app.BackupController
import com.audioshelf.app.R
import com.audioshelf.app.ui.components.AText
import com.audioshelf.app.ui.components.Actions
import com.audioshelf.app.ui.components.Btn
import com.audioshelf.app.ui.components.ModalView
import com.audioshelf.app.ui.components.OverlayHost
import com.audioshelf.app.ui.components.message
import com.audioshelf.app.ui.theme.AppTheme
import kotlinx.coroutines.launch

class BackupLaunchers(val save: () -> Unit, val restore: () -> Unit)

/** The two system file pickers of the backup, and what follows each result. */
@Composable
fun rememberBackupLaunchers(controller: BackupController, overlays: OverlayHost): BackupLaunchers {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch { overlays.message(if (controller.save(uri)) R.string.backup_saved else R.string.backup_failed) }
        }
    }
    val loader = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val rows = controller.read(uri)
                if (rows == null) {
                    overlays.message(R.string.backup_bad)
                } else {
                    controller.restore(rows)
                    overlays.show { RestartDialog { controller.exit(context.findActivity()) } }
                }
            }
        }
    }
    return remember(saver, loader) {
        BackupLaunchers(
            save = { saver.launch("audioshelf-backup.json") },
            restore = { loader.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
        )
    }
}

@Composable
private fun RestartDialog(onClose: () -> Unit) {
    ModalView(stringResource(R.string.msg_title), {}) {
        AText(stringResource(R.string.backup_restored), 16f, AppTheme.colors.text, weight = FontWeight.SemiBold)
        Actions { Btn(stringResource(R.string.backup_close_app), onClose) }
    }
}
