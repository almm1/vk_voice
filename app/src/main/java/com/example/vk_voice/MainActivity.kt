package com.example.vk_voice

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalProvider
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vk_voice.ui.theme.Typography
import com.example.vk_voice.ui.theme.Vk_voiceTheme
import com.vk.api.sdk.VK
import com.vk.api.sdk.auth.VKAuthenticationResult
import com.vk.api.sdk.auth.VKScope
import kotlinx.coroutines.launch
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction3


class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.ViewModelFactory(
            applicationContext
        )
    }
    private val requestRecordAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ::onGotRecordAudioPermissionResult
    )
    private val authLauncher = VK.login(this) { result: VKAuthenticationResult ->
        when (result) {
            is VKAuthenticationResult.Success -> {
                // User passed authorization
            }
            is VKAuthenticationResult.Failed -> {
                // User didn't pass authorization
            }
        }
    }

    private fun onGotRecordAudioPermissionResult(granted: Boolean) {}

    private fun clickRecord() {
        viewModel.record()
    }


    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContent {
            MApp()
        }
        requestRecordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        val filePath = "${externalCacheDir?.absolutePath}"

        viewModel.initListRecords()
        viewModel.defaultPath.value = filePath
        viewModel.initRecordState()
        viewModel.initTimer()
    }

    @ExperimentalMaterialApi
    @Composable
    private fun MApp() {
        val scaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed)
        )
        val coroutineScope = rememberCoroutineScope()

        Vk_voiceTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                BottomSheetScaffold(
                    sheetContent = {
                        Row(
                            modifier = Modifier
                                .background(Color.White)
                                .fillMaxWidth()
                                .fillMaxHeight(0.15f)
                                .padding(8.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Запись...")
                            Text(viewModel.formatDuration(viewModel.timeRecord.value))
                        }
                    },
                    scaffoldState = scaffoldState,
                    floatingActionButtonPosition = FabPosition.Center,
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                if (viewModel.mStartPlaying.all { !it }) {
                                    coroutineScope.launch {
                                        if (scaffoldState.bottomSheetState.isCollapsed) {
                                            scaffoldState.bottomSheetState.expand()
                                        } else {
                                            scaffoldState.bottomSheetState.collapse()
                                        }
                                    }
                                    clickRecord()
                                }
                            },
                            Modifier.size(56.dp),
                            contentColor = Color.White,
                            backgroundColor = MaterialTheme.colors.primary
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_launcher_foreground),
                                contentDescription = "fab icon",
                            )
                        }
                    },
                    content = {
                        RecordsList()
                    }
                )
            }
        }
    }

    @Composable
    fun RecordsList() {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Ваши записи",
                    modifier = Modifier.padding(16.dp),
                    style = Typography.h1
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { authLauncher.launch(arrayListOf(VKScope.DOCS)) }) {
                    Text(text = "Авторизация")
                }
            }
            Spacer(modifier = Modifier.size(24.dp))
            LazyColumn(Modifier.padding(bottom = 100.dp)) {
                items(viewModel.listRecords.size) {
                    RecordItem(it)
                }
            }
        }
    }

    @Composable
    fun DropDownMenu(record: Record, i: Int) {
        var expanded by rememberSaveable { mutableStateOf(false) }
        Box {
            IconButton(
                onClick = { expanded = true },
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Показать меню")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(onClick = { viewModel.openDialog(i) }) {
                    Icon(Icons.Default.Edit, contentDescription = "rename")
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Переименовать", softWrap = false)
                }
                DropdownMenuItem(onClick = { viewModel.delete(record.path) }) {
                    Icon(Icons.Default.Delete, contentDescription = "delete")
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Удалить")
                }
            }
        }
    }

    @Composable
    fun AlertDialog(
        show: Boolean,
        onConfirm: KFunction3<Int, String, String, Unit>,
        onDismiss: KFunction1<Int, Unit>,
        viewModel: MainViewModel,
        i: Int
    ) {
        var text: String by rememberSaveable { mutableStateOf(viewModel.listRecords[i].name) }

        if (show) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {
                    Row {
                        OutlinedTextField(
                            onValueChange = { text = it },
                            singleLine = true,
                            value = text,
                            modifier = Modifier.fillMaxWidth(0.5f)
                        )
                        TextButton(onClick = { onConfirm(i, viewModel.listRecords[i].path, text) })
                        { Text(text = "Ок", color = Color.Black) }
                        TextButton(onClick = { onDismiss(i) })
                        { Text(text = "Отмена", color = Color.Black) }
                    }
                },
            )
        }
    }

    @Composable
    fun RecordItem(i: Int) {
        val showDialogState = viewModel.showDialog
        if (showDialogState[i]) {
            AlertDialog(
                show = showDialogState[i],
                onConfirm = viewModel::onDialogConfirm,
                viewModel = viewModel,
                onDismiss = viewModel::onDialogDismiss,
                i = i
            )
        }
        Card(
            backgroundColor = MaterialTheme.colors.secondaryVariant,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(viewModel.listRecords[i].name, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        Text(viewModel.listRecords[i].time, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(viewModel.listRecords[i].length)
                        Spacer(modifier = Modifier.size(8.dp))
                        when (viewModel.mStartPlaying[i]) {
                            true -> IconButton(
                                onClick = {
                                    if (!viewModel.mStartRecording.value) {
                                        viewModel.clickPlay(viewModel.listRecords[i].path, i)
                                    }
                                },
                                content = {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        tint = Color.White,
                                        contentDescription = "play",
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colors.primary,
                                                shape = CircleShape
                                            )
                                            .padding(4.dp),
                                    )
                                },
                            )
                            false -> IconButton(
                                onClick = {
                                    if (!viewModel.mStartRecording.value) {
                                        viewModel.clickPlay(viewModel.listRecords[i].path, i)
                                    }
                                },
                                content = {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        tint = Color.White,
                                        contentDescription = "pause",
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colors.secondary,
                                                shape = CircleShape
                                            )
                                            .padding(4.dp),
                                    )
                                },
                            )
                        }
                        DropDownMenu(viewModel.listRecords[i], i)
                    }
                }
            }
        }
    }

//SHA1: 8C:92:FC:6B:A6:B5:06:D3:F3:B4:06:CF:3B:D7:C9:9F:3B:1A:3A:8F
}

