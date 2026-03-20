package com.haru.ocrkeyboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.haru.ocrkeyboard.ui.theme.OCRKeyboardTheme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

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
                            title = { Text("OCR Keyboard Test") }
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
 * シリアルコードなどのOCR入力確認用UI
 *
 * @param modifier 修飾子
 */
@Composable
fun TestInputScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settingsStore = remember { com.haru.ocrkeyboard.data.local.SettingsStore(context) }
    
    var text by remember { mutableStateOf("") }
    var useSwipeGesture by remember { mutableStateOf(settingsStore.useSwipeGesture) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "この画面でキーボードを「OCR Keyboard」に切り替えて、シリアルコードの入力をテストできます。",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
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
                        useSwipeGesture = false
                        settingsStore.useSwipeGesture = false
                    },
                    role = Role.RadioButton
                )
                .padding(vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            RadioButton(
                selected = !useSwipeGesture,
                onClick = null // RowのModifier.selectableに委譲
            )
            Text(text = "ピンチ操作 (2本指で拡大縮小)", modifier = Modifier.padding(start = 8.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = useSwipeGesture,
                    onClick = { 
                        useSwipeGesture = true
                        settingsStore.useSwipeGesture = true
                    },
                    role = Role.RadioButton
                )
                .padding(vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            RadioButton(
                selected = useSwipeGesture,
                onClick = null // RowのModifier.selectableに委譲
            )
            Text(text = "スワイプ操作 (1本指で上下左右)", modifier = Modifier.padding(start = 8.dp))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
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
 * テキスト入力テスト用画面のプレビュー
 *
 * 画面のデフォルト状態の描画確認用
 */
@Preview(showBackground = true)
@Composable
private fun TestInputScreenPreview() {
    OCRKeyboardTheme {
        TestInputScreen()
    }
}