package com.example.ui.screens

import com.example.BuildConfig
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.StopSyncState
import com.example.ui.theme.AppThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsDialog(
  currentTheme: AppThemeMode,
  cachedStopCount: Int,
  syncState: StopSyncState,
  currentApiKey: String,
  syncCooldownSeconds: Int = 0,
  onThemeSelected: (AppThemeMode) -> Unit,
  onSyncStops: () -> Unit,
  onApiKeySaved: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var apiKeyInput by remember { mutableStateOf(currentApiKey) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "MBTA Live Settings",
        style = MaterialTheme.typography.titleLarge
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        // Theme selection
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Brightness4, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "App Theme",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AppThemeMode.values().forEach { mode ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onThemeSelected(mode) }
              .padding(vertical = 4.dp)
          ) {
            RadioButton(
              selected = mode == currentTheme,
              onClick = { onThemeSelected(mode) }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = when (mode) {
                AppThemeMode.SYSTEM -> "System Default (Follows OS)"
                AppThemeMode.LIGHT -> "Light Mode"
                AppThemeMode.DARK -> "Dark Mode (High Contrast)"
              },
              style = MaterialTheme.typography.bodyMedium
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Offline Transit Database Management
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("offline_database_card")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (syncState is StopSyncState.Syncing) Icons.Default.CloudSync else Icons.Default.CloudDone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Offline Transit Database",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Full MBTA system synced locally",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Cached Stops",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = if (cachedStopCount > 0) "$cachedStopCount stops" else "No stops cached",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                  )
                  val statusText = when (syncState) {
                    is StopSyncState.Syncing -> "Downloading all stops from MBTA API..."
                    is StopSyncState.Success -> {
                      val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.US)
                      "Last updated: ${sdf.format(Date(syncState.epochMs))}"
                    }
                    is StopSyncState.Error -> syncState.message
                    StopSyncState.Idle -> if (cachedStopCount > 0) "Ready for offline use" else "Tap Sync to download"
                  }
                  Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (syncState is StopSyncState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (syncState is StopSyncState.Syncing) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp
                  )
                } else {
                  FilledTonalButton(
                    onClick = onSyncStops,
                    enabled = syncCooldownSeconds == 0,
                    modifier = Modifier.testTag("sync_stops_button")
                  ) {
                    Icon(
                      Icons.Default.Refresh,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = if (syncCooldownSeconds > 0) "Wait ${syncCooldownSeconds}s" else "Sync Now",
                      fontSize = 12.sp
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom API Key
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "MBTA V3 API Key (Optional)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }
        Text(
          text = "Free public tier allows 20 req/min. Supplying a free registered key from api-v3.mbta.com increases quota to 1,000 req/min.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = apiKeyInput,
          onValueChange = { apiKeyInput = it },
          placeholder = { Text("Paste optional API key") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
          onClick = { onApiKeySaved(apiKeyInput.trim()) },
          modifier = Modifier.align(Alignment.End)
        ) {
          Text("Save Key")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // API Cost Info
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
          ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(
              text = "100% Free Open Data: All MBTA data is provided free of charge by MassDOT. Total monthly cost: $0.00.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // App Version
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("app_version_field")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "App Version",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
              )
            }
            Text(
              text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done")
      }
    },
    modifier = Modifier.testTag("settings_dialog")
  )
}
