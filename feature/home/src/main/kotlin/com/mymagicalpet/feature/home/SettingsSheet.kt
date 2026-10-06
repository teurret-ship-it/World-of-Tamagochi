package com.mymagicalpet.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mymagicalpet.ui.time.TimeStepper
import com.mymagicalpet.ui.time.clockTime
import com.mymagicalpet.ui.time.shiftTime

/** Sound and vibration toggles (CLAUDE.md section 2) and credits for the assets. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsSheet(
    state: HomeUiState,
    actions: HomeActions,
    onClose: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium)
            Toggle(stringResource(R.string.settings_sound), state.sound, actions.onSound)
            Toggle(stringResource(R.string.settings_haptics), state.haptics, actions.onHaptics)
            Text(
                stringResource(R.string.settings_sleep_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(stringResource(R.string.settings_sleep_hint, state.name), style = MaterialTheme.typography.bodyLarge)
            TimeStepper(stringResource(R.string.settings_bedtime), state.bedtimeMinute) { actions.onSleepWindow(it, state.wakeMinute) }
            TimeStepper(stringResource(R.string.settings_wake_up), state.wakeMinute) { actions.onSleepWindow(state.bedtimeMinute, it) }
            Text(
                stringResource(R.string.settings_vacation_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(stringResource(R.string.settings_vacation_hint, state.name), style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VACATIONS.forEach { (days, label) ->
                    OutlinedButton(onClick = { actions.onAway(AwayAction.Vacation(days)) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(label))
                    }
                }
            }
            Text(
                stringResource(R.string.settings_credits_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(stringResource(R.string.settings_credits), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun Toggle(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** Vacation lengths offered, in days, up to the two weeks CLAUDE.md allows. */
private val VACATIONS = listOf(3 to R.string.settings_vacation_3, 7 to R.string.settings_vacation_7, 14 to R.string.settings_vacation_14)
