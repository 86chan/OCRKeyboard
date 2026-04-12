package com.haru.ocrkeyboard

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.haru.ocrkeyboard.data.local.SettingsRepository
import com.haru.ocrkeyboard.domain.model.CharReplacement
import com.haru.ocrkeyboard.ui.theme.OCRKeyboardTheme
import kotlinx.coroutines.launch

/**
 * アプリのメインアクティビティ
 */
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OCRKeyboardTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("OCR Keyboard") }
                        )
                    }
                ) { innerPadding ->
                    TestInputScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/**
 * テキスト入力テスト用画面
 *
 * キーボードの有効化案内、カメラ権限のリクエスト、設定変更、入力テスト用UIの提供
 *
 * @param modifier 修飾子
 */
@Composable
fun TestInputScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsRepository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf("") }
    val useSwipeGesture by settingsRepository.useSwipeGestureFlow.collectAsStateWithLifecycle(initialValue = false)
    val useJapanese by settingsRepository.useJapaneseRecognitionFlow.collectAsStateWithLifecycle(initialValue = false)
    val charReplacements by settingsRepository.charReplacementsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // カメラ権限の状態管理
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // 設定画面から戻ってきた際などに権限状態を再確認
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "1. キーボードの有効化",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "設定アプリで「OCR Keyboard」を有効にする必要があります。",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                } catch (e: ActivityNotFoundException) {
                    // 設定画面を開けない場合の例外処理
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("システム設定を開く")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

        Text(
            text = "2. カメラ権限の許可",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "OCR機能を利用するためにカメラへのアクセス権限が必要です。",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (!hasCameraPermission) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = if (hasCameraPermission) {
                ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                )
            } else {
                ButtonDefaults.buttonColors()
            }
        ) {
            if (hasCameraPermission) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("カメラ権限 許可済み")
            } else {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("カメラ権限をリクエスト")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

        Text(
            text = "3. キーボードの設定",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "枠サイズ変更のジェスチャー操作",
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = !useSwipeGesture,
                    onClick = {
                        scope.launch { settingsRepository.setUseSwipeGesture(false) }
                    },
                    role = Role.RadioButton
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = !useSwipeGesture,
                onClick = null
            )
            Text(text = "ピンチ操作 (2本指で拡大縮小)", modifier = Modifier.padding(start = 8.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = useSwipeGesture,
                    onClick = {
                        scope.launch { settingsRepository.setUseSwipeGesture(true) }
                    },
                    role = Role.RadioButton
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = useSwipeGesture,
                onClick = null
            )
            Text(text = "スワイプ操作 (1本指で上下左右)", modifier = Modifier.padding(start = 8.dp))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "日本語認識を有効にする",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "オフの場合は英数字のみを認識します。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = useJapanese,
                onCheckedChange = {
                    scope.launch { settingsRepository.setUseJapaneseRecognition(it) }
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        CharReplacementSection(
            replacements = charReplacements,
            onReplacementsChanged = { updated ->
                scope.launch { settingsRepository.setCharReplacements(updated) }
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

        Text(
            text = "4. 入力テスト",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "この画面でキーボードを切り替えて、シリアルコードの入力をテストできます。",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("シリアルコード入力欄") },
            placeholder = { Text("ここをタップして入力...") },
            singleLine = false,
            maxLines = 5
        )
    }
}

/**
 * 文字置換マッピング設定セクション
 *
 * 各ルールのチェックボックス（有効/無効）・from・to・削除ボタンをリスト表示し、
 * 追加ボタンで新規ルールを末尾に追加できる
 *
 * @param replacements 現在の置換ルール一覧
 * @param onReplacementsChanged ルールが変更されたときのコールバック
 */
@Composable
private fun CharReplacementSection(
    replacements: List<CharReplacement>,
    onReplacementsChanged: (List<CharReplacement>) -> Unit,
) {
    Text(
        text = "文字置換マッピング",
        style = MaterialTheme.typography.titleMedium
    )
    Text(
        text = "OCR認識後に自動で置き換える文字列のルールを設定します。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(8.dp))

    replacements.forEachIndexed { index, rule ->
        CharReplacementRow(
            rule = rule,
            onEnabledChanged = { isEnabled ->
                onReplacementsChanged(
                    replacements.toMutableList().also { it[index] = rule.copy(isEnabled = isEnabled) }
                )
            },
            onFromChanged = { from ->
                onReplacementsChanged(
                    replacements.toMutableList().also { it[index] = rule.copy(from = from) }
                )
            },
            onToChanged = { to ->
                onReplacementsChanged(
                    replacements.toMutableList().also { it[index] = rule.copy(to = to) }
                )
            },
            onDelete = {
                onReplacementsChanged(replacements.toMutableList().also { it.removeAt(index) })
            },
        )
        Spacer(modifier = Modifier.height(4.dp))
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
        onClick = {
            onReplacementsChanged(replacements + CharReplacement(from = "", to = "", isEnabled = true))
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("追加")
    }
}

/**
 * 文字置換ルール1行分のUI
 *
 * チェックボックス（有効/無効）・認識文字フィールド・置換後文字フィールド・削除ボタンで構成する
 * テキストの変更はフォーカスが外れたタイミングで保存し、不要なIO負荷を避ける
 *
 * @param rule 表示対象のルール
 * @param onEnabledChanged 有効/無効変更時のコールバック
 * @param onFromChanged 認識文字変更時（フォーカスアウト）のコールバック
 * @param onToChanged 置換後文字変更時（フォーカスアウト）のコールバック
 * @param onDelete 削除ボタン押下時のコールバック
 */
@Composable
private fun CharReplacementRow(
    rule: CharReplacement,
    onEnabledChanged: (Boolean) -> Unit,
    onFromChanged: (String) -> Unit,
    onToChanged: (String) -> Unit,
    onDelete: () -> Unit,
) {
    // フォーカスアウト時に保存するため、ローカルのedit用stateを持つ
    var fromText by remember(rule.from) { mutableStateOf(rule.from) }
    var toText by remember(rule.to) { mutableStateOf(rule.to) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("ルールの削除") },
            text = { Text("この文字置換ルールを削除しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    }
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text("キャンセル")
                }
            }
        )
    }

    val disabledAlpha = 0.38f
    val contentAlpha = if (rule.isEnabled) 1f else disabledAlpha

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = rule.isEnabled,
            onCheckedChange = onEnabledChanged,
        )

        OutlinedTextField(
            value = fromText,
            onValueChange = { fromText = it },
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState ->
                    // フォーカスを失ったタイミングのみ保存してIOを削減する
                    if (!focusState.isFocused && fromText != rule.from) {
                        onFromChanged(fromText)
                    }
                },
            label = { Text("認識") },
            singleLine = true,
            enabled = rule.isEnabled,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            ),
        )

        Text(
            text = "→",
            modifier = Modifier.padding(horizontal = 4.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
        )

        OutlinedTextField(
            value = toText,
            onValueChange = { toText = it },
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && toText != rule.to) {
                        onToChanged(toText)
                    }
                },
            label = { Text("置換後") },
            singleLine = true,
            enabled = rule.isEnabled,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            ),
        )

        IconButton(onClick = { showDeleteConfirmDialog = true }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "削除",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/**
 * テキスト入力テスト用画面のプレビュー
 */
@Preview(showBackground = true)
@Composable
private fun TestInputScreenPreview() {
    OCRKeyboardTheme {
        TestInputScreen()
    }
}

/**
 * 文字置換セクションのプレビュー（ルールあり）
 */
@Preview(showBackground = true)
@Composable
private fun CharReplacementSectionPreview() {
    OCRKeyboardTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CharReplacementSection(
                replacements = listOf(
                    CharReplacement(from = "Ø", to = "0", isEnabled = true),
                    CharReplacement(from = "ø", to = "0", isEnabled = true),
                    CharReplacement(from = "I", to = "1", isEnabled = false),
                ),
                onReplacementsChanged = {}
            )
        }
    }
}

/**
 * 文字置換セクションのプレビュー（空リスト）
 */
@Preview(showBackground = true)
@Composable
private fun CharReplacementSectionEmptyPreview() {
    OCRKeyboardTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CharReplacementSection(
                replacements = emptyList(),
                onReplacementsChanged = {}
            )
        }
    }
}
