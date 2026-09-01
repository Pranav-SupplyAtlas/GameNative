package app.gamenative.ui.component.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.gamenative.R
import app.gamenative.utils.ContainerCompatibilityAnalyzer
import app.gamenative.utils.ContainerSelectionCoordinator

@Composable
fun ContainerSelectionDialog(
    requestedSummary: String,
    containers: List<Pair<ContainerSelectionCoordinator.ExistingContainer, ContainerCompatibilityAnalyzer.Result>>,
    onDismiss: () -> Unit,
    onCommit: (ContainerSelectionCoordinator.Choice) -> Unit,
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var conflict by remember { mutableStateOf<ContainerCompatibilityAnalyzer.Result.SharedBaseConflict?>(null) }
    if (conflict != null && selectedId != null) {
        AlertDialog(
            onDismissRequest = { conflict = null },
            title = { Text(stringResource(R.string.shared_container_conflict_title)) },
            text = { Text(stringResource(R.string.shared_container_conflict_message, conflict!!.reasons.joinToString())) },
            confirmButton = {
                TextButton(onClick = { onCommit(ContainerSelectionCoordinator.Choice.CreateNewContainer) }) {
                    Text(stringResource(R.string.create_recommended_container))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        onCommit(ContainerSelectionCoordinator.Choice.UseExistingContainer(selectedId!!, true))
                    }) { Text(stringResource(R.string.retain_shared_container_base)) }
                    TextButton(onClick = { conflict = null }) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_game_container)) },
        text = {
            Column {
                Row(Modifier.fillMaxWidth().clickable { selectedId = null }.padding(8.dp)) {
                    RadioButton(selected = selectedId == null, onClick = { selectedId = null })
                    Column { Text(stringResource(R.string.create_new_container_recommended)); Text(requestedSummary) }
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.use_existing_container))
                containers.forEach { (item, result) ->
                    Row(Modifier.fillMaxWidth().clickable { selectedId = item.id }.padding(8.dp)) {
                        RadioButton(selected = selectedId == item.id, onClick = { selectedId = item.id })
                        Column {
                            Text(item.name)
                            Text(stringResource(R.string.shared_container_row_summary, item.linkedGames, item.summary))
                            Text(when (result) {
                                ContainerCompatibilityAnalyzer.Result.Compatible -> stringResource(R.string.container_compatible)
                                is ContainerCompatibilityAnalyzer.Result.LaunchProfileOnly -> stringResource(R.string.container_profile_difference)
                                is ContainerCompatibilityAnalyzer.Result.SharedBaseConflict -> stringResource(R.string.container_base_conflict)
                            })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val id = selectedId
                if (id == null) onCommit(ContainerSelectionCoordinator.Choice.CreateNewContainer)
                else {
                    val result = containers.first { it.first.id == id }.second
                    if (result is ContainerCompatibilityAnalyzer.Result.SharedBaseConflict) conflict = result
                    else onCommit(ContainerSelectionCoordinator.Choice.UseExistingContainer(id))
                }
            }) { Text(stringResource(R.string.continue_text)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
