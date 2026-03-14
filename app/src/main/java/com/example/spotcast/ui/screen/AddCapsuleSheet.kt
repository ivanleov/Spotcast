package com.example.spotcast.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.spotcast.R
import com.example.spotcast.SpotCastApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCapsuleSheet(
    latitude: Double,
    longitude: Double,
    onDismiss: () -> Unit,
    onSaveText: (lat: Double, lon: Double, radius: Double, text: String, layer: String, ttlHours: Int?, recipientUsername: String?) -> Unit,
    onSaveAudio: (lat: Double, lon: Double, radius: Float, layer: String, audioPath: String) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as SpotCastApplication
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var capsuleType by remember { mutableStateOf("TEXT") }
    var textContent by remember { mutableStateOf("") }
    var layer by remember { mutableStateOf("personal") }
    var radiusStr by remember { mutableStateOf("50") }
    var ttlStr by remember { mutableStateOf("") }
    var recipientStr by remember { mutableStateOf("") }

    var isRecording by remember { mutableStateOf(false) }
    var recordedPath by remember { mutableStateOf<String?>(null) }

    val audioPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && !isRecording) {
            recordedPath = app.audioRecorder.startRecording()
            isRecording = true
        }
    }

    val layers = listOf("personal", "work", "city", "logistics", "social")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.new_capsule),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "%.5f, %.5f".format(latitude, longitude),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            Row {
                FilterChip(
                    selected = capsuleType == "TEXT",
                    onClick = { capsuleType = "TEXT" },
                    label = { Text(stringResource(R.string.text)) },
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = capsuleType == "AUDIO",
                    onClick = { capsuleType = "AUDIO" },
                    label = { Text(stringResource(R.string.audio)) },
                )
            }
            Spacer(Modifier.height(16.dp))

            if (capsuleType == "TEXT") {
                OutlinedTextField(
                    value = textContent,
                    onValueChange = { textContent = it },
                    label = { Text(stringResource(R.string.note)) },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                if (isRecording) {
                    OutlinedButton(
                        onClick = {
                            recordedPath = app.audioRecorder.stopRecording()
                            isRecording = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("⏹  " + stringResource(R.string.stop_recording))
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (recordedPath != null) "🎤  " + stringResource(R.string.re_record) else "🎤  " + stringResource(R.string.start_recording))
                    }
                }
                if (recordedPath != null && !isRecording) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "✓ " + stringResource(R.string.audio_recorded),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.layer), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                layers.take(4).forEach { l ->
                    FilterChip(
                        selected = layer == l,
                        onClick = { layer = l },
                        label = { Text(l.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = radiusStr,
                    onValueChange = { radiusStr = it },
                    label = { Text(stringResource(R.string.radius_m)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = ttlStr,
                    onValueChange = { ttlStr = it },
                    label = { Text(stringResource(R.string.ttl_hours)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = recipientStr,
                onValueChange = { recipientStr = it },
                label = { Text(stringResource(R.string.recipient_optional)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val radius = radiusStr.toDoubleOrNull() ?: 50.0
                    val ttl = ttlStr.toIntOrNull()
                    val recipient = recipientStr.trim().ifBlank { null }

                    if (capsuleType == "TEXT" && textContent.isNotBlank()) {
                        onSaveText(latitude, longitude, radius, textContent, layer, ttl, recipient)
                    } else if (capsuleType == "AUDIO" && recordedPath != null) {
                        onSaveAudio(latitude, longitude, radius.toFloat(), layer, recordedPath!!)
                    }
                },
                enabled = when (capsuleType) {
                    "TEXT" -> textContent.isNotBlank()
                    "AUDIO" -> recordedPath != null && !isRecording
                    else -> false
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.save_capsule), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
