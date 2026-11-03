package com.audioshelf.app.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.audioshelf.app.AppLog
import com.audioshelf.app.R
import com.audioshelf.app.diag.LogShare
import com.audioshelf.app.ui.components.OverlayHost
import com.audioshelf.app.ui.components.message
import kotlinx.coroutines.launch

class LogActions(val share: () -> Unit, val clear: () -> Unit)

/** Share the diagnostic log through the system share sheet, or start it over. */
@Composable
fun rememberLogActions(logShare: LogShare, overlays: OverlayHost): LogActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val title = stringResource(R.string.log_share_title)
    return remember(logShare, overlays, context, title) {
        LogActions(
            share = {
                scope.launch {
                    val uri = logShare.prepare()
                    if (uri == null) {
                        overlays.message(R.string.log_failed)
                    } else {
                        val host = context.findActivity() ?: context
                        val intent = logShare.chooser(uri, title)
                        if (host !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        try {
                            host.startActivity(intent)
                        } catch (e: Exception) {
                            AppLog.e("LogShare", "no app could take the log", e)
                            overlays.message(R.string.log_failed)
                        }
                    }
                }
            },
            clear = {
                logShare.clear()
                overlays.message(R.string.log_cleared)
            },
        )
    }
}
