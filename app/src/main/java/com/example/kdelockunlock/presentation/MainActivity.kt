package com.example.kdelockunlock.presentation

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.example.kdelockunlock.data.WatchBluetoothClient
import com.example.kdelockunlock.presentation.theme.KdeLockUnlockTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val command = intent?.getStringExtra("command")
        setContent {
            KdeLockUnlockApp(initialCommand = command)
        }
    }
}

@Composable
fun KdeLockUnlockApp(initialCommand: String? = null) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val btClient = remember { WatchBluetoothClient(context) }
    val coroutineScope = rememberCoroutineScope()

    val statusText by btClient.statusText.collectAsState()
    val isConnected by btClient.isConnected.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (initialCommand == "LOCK") {
            btClient.sendLockCommand()
        } else if (initialCommand == "UNLOCK") {
            btClient.sendUnlockCommand()
        }
    }

    KdeLockUnlockTheme {
        AppScaffold {
            val listState = rememberTransformingLazyColumnState()
            val transformationSpec = rememberTransformationSpec()
            val focusRequester = remember { FocusRequester() }

            DisposableEffect(Unit) {
                focusRequester.requestFocus()
                onDispose { }
            }

            ScreenScaffold(
                scrollState = listState,
                scrollIndicator = {
                    ScrollIndicator(state = listState)
                }
            ) { contentPadding ->
                TransformingLazyColumn(
                    contentPadding = contentPadding,
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .focusable()
                        .onRotaryScrollEvent { event ->
                            coroutineScope.launch {
                                listState.scrollBy(event.verticalScrollPixels)
                            }
                            true
                        }
                ) {
                    item {
                        ListHeader(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec)
                        ) {
                            Text(
                                text = "BT PC Control",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }

                    item {
                        Card(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isConnected) {
                                    MaterialTheme.colorScheme.surfaceContainer
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    btClient.sendLockCommand()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text("🔒 Lock PC", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    btClient.sendUnlockCommand()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("🔓 Unlock PC", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec)
                        ) {
                            Text("BT Connected", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@WearPreviewDevices
@Composable
fun MainPreview() {
    KdeLockUnlockApp()
}
