package com.hubclub.iosgallery.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.hubclub.iosgallery.model.MediaItem
import com.hubclub.iosgallery.model.TrashEntry
import kotlinx.coroutines.launch

/**
 * Shared "move to Recently Deleted" flow.
 *
 * The bytes are copied into the app's private trash first, then Android is asked
 * to remove the original. When the system shows its own confirmation dialog and
 * the user declines, the copies are rolled back so nothing is left behind.
 */
@Composable
fun rememberTrashFlow(
    vm: GalleryViewModel,
    onMessage: (String) -> Unit
): (List<MediaItem>) -> Unit {
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<List<TrashEntry>>(emptyList()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onMessage("Moved to Recently Deleted")
        } else {
            vm.discardTrash(pending)
            onMessage("Nothing was deleted")
        }
        pending = emptyList()
        vm.refresh()
    }

    return remember(vm) {
        { targets: List<MediaItem> ->
            if (targets.isNotEmpty()) {
                scope.launch {
                    val outcome = vm.moveToTrash(targets)
                    pending = outcome.entries
                    val consent = outcome.consent
                    if (consent != null) {
                        runCatching {
                            launcher.launch(IntentSenderRequest.Builder(consent).build())
                        }.onFailure {
                            vm.discardTrash(outcome.entries)
                            onMessage("Nothing was deleted")
                        }
                    } else if (outcome.entries.isEmpty()) {
                        onMessage("Couldn't move that item")
                    } else {
                        onMessage("Moved to Recently Deleted")
                        vm.refresh()
                    }
                }
            }
        }
    }
}

