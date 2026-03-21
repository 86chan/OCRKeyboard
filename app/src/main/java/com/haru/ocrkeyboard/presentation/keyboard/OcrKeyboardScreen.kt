package com.haru.ocrkeyboard.presentation.keyboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.KeyboardTab
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.haru.ocrkeyboard.presentation.keyboard.components.CameraPreview
import com.haru.ocrkeyboard.presentation.keyboard.components.takePicture
import kotlin.math.abs
import kotlin.math.atan2
import kotlinx.coroutines.delay

/**
 * OCRキーボードのメイン画面
 *
 * 権限管理およびカメラ制御のライフサイクル統合を担当
 *
 * @param state UI状態
 * @param onIntent インテント発行用コールバック
 * @param modifier 修飾子
 */
@Composable
fun OcrKeyboardScreen(
    state: OcrKeyboardState,
    onIntent: (OcrKeyboardIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    /** カメラ権限の付与状態 */
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    /** ライフサイクルイベント監視による権限の動的再チェック */
    val lifecycleOwner = LocalLifecycleOwner.current
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

    if (!hasCameraPermission) {
        PermissionRequiredContent(
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            },
            modifier = modifier
        )
        return
    }

    /** 画像キャプチャ用インスタンスの保持 */
    val imageCapture = remember { ImageCapture.Builder().build() }
    
    OcrKeyboardContent(
        state = state,
        onIntent = onIntent,
        cameraPreview = { previewModifier ->
            CameraPreview(
                imageCapture = imageCapture,
                onCameraReady = { onIntent(OcrKeyboardIntent.CameraReady) },
                modifier = previewModifier
            )
        },
        onCapture = { width, height, isJapaneseEnabled, boxWidth, boxHeight, boxTop ->
            val executor = ContextCompat.getMainExecutor(context)
            takePicture(
                imageCapture = imageCapture,
                executor = executor,
                onCaptured = { bytes, rotation ->
                    onIntent(
                        OcrKeyboardIntent.RecognizeText(
                            bytes, rotation, isJapaneseEnabled, width, height, boxWidth, boxHeight, boxTop
                        )
                    )
                },
                onError = { /* エラー通知ロジック等 */ }
            )
        },
        onOpenApp = {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent != null) {
                context.startActivity(intent)
            }
        },
        modifier = modifier
    )
}

/**
 * キーボードのUIレイアウト
 *
 * 画面構成要素の配置およびジェスチャー検知を管理
 *
 * @param state UI状態
 * @param onIntent インテント発行用コールバック
 * @param cameraPreview カメラプレビュー表示用Composable
 * @param onCapture シャッター実行時のコールバック
 * @param onOpenApp 設定アプリを開くコールバック
 * @param modifier 修飾子
 */
@Composable
private fun OcrKeyboardContent(
    state: OcrKeyboardState,
    onIntent: (OcrKeyboardIntent) -> Unit,
    cameraPreview: @Composable (Modifier) -> Unit,
    onCapture: (Int, Int, Boolean, Float, Float, Float) -> Unit,
    onOpenApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    /** プレビュー表示幅の計測値 */
    var previewWidth by remember { mutableIntStateOf(0) }
    /** プレビュー表示高さの計測値 */
    var previewHeight by remember { mutableIntStateOf(0) }
    /** スキャン枠の横幅比率 */
    var boxWidthRatio by remember { mutableFloatStateOf(0.8f) }
    /** スキャン枠の高さ比率 */
    var boxHeightRatio by remember { mutableFloatStateOf(0.15f) }
    /** スキャン枠の上部オフセット比率 */
    val boxTopRatio = 0.2f

    /** 削除ボタンの継続押下フラグ */
    var isDeletePressed by remember { mutableStateOf(false) }

    /** 削除ボタンのオートリピートタイマー管理 */
    LaunchedEffect(isDeletePressed) {
        if (isDeletePressed) {
            onIntent(OcrKeyboardIntent.DeleteKeyPressed)
            delay(400)
            while (isDeletePressed) {
                onIntent(OcrKeyboardIntent.DeleteKeyPressed)
                delay(50)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(500.dp)
            .background(Color.Black)
            .onGloballyPositioned {
                previewWidth = it.size.width
                previewHeight = it.size.height
            }
            .pointerInput(state.useSwipeGesture) {
                handleGesture(
                    useSwipeGesture = state.useSwipeGesture,
                    onUpdateBox = { dw, dh ->
                        boxWidthRatio = (boxWidthRatio + dw).coerceIn(0.2f, 1.0f)
                        boxHeightRatio = (boxHeightRatio + dh).coerceIn(0.05f, 0.8f)
                    }
                )
            }
    ) {
        cameraPreview(Modifier.fillMaxSize())

        ScanningOverlay(
            boxWidthRatio = boxWidthRatio,
            boxHeightRatio = boxHeightRatio,
            boxTopRatio = boxTopRatio
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.suggestionCandidates.isNotEmpty()) {
                SuggestionRow(
                    candidates = state.suggestionCandidates,
                    onSelected = { onIntent(OcrKeyboardIntent.SuggestionSelected(it)) },
                    modifier = Modifier.padding(top = 0.dp)
                )
            }
            StatusText(
                errorMessage = state.errorMessage,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        if (state.isRecognizing) {
            LoadingOverlay()
        } else {
            KeyboardControls(
                isDeletePressed = isDeletePressed,
                onDeletePressChange = { isDeletePressed = it },
                onCaptureClick = {
                    onCapture(previewWidth, previewHeight, state.useJapanese, boxWidthRatio, boxHeightRatio, boxTopRatio)
                },
                onNextClick = { onIntent(OcrKeyboardIntent.NextKeyPressed) },
                onEnterClick = { onIntent(OcrKeyboardIntent.EnterKeyPressed) },
                onOpenApp = onOpenApp,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * 入力候補の表示行
 * 
 * @param candidates 候補リスト
 * @param onSelected 選択時のコールバック
 * @param modifier 修飾子
 */
@Composable
private fun SuggestionRow(
    candidates: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(candidates) { text ->
            Surface(
                onClick = { onSelected(text) },
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.9f),
                tonalElevation = 2.dp
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            }
        }
    }
}

/**
 * スキャン範囲を示す透過ターゲット枠
 *
 * @param boxWidthRatio 枠幅比率
 * @param boxHeightRatio 枠高さ比率
 * @param boxTopRatio 枠上部位置比率
 */
@Composable
private fun ScanningOverlay(
    boxWidthRatio: Float,
    boxHeightRatio: Float,
    boxTopRatio: Float
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 0.99f }
    ) {
        val boxWidth = size.width * boxWidthRatio
        val boxHeight = size.height * boxHeightRatio
        val left = (size.width - boxWidth) / 2
        val top = size.height * boxTopRatio
        val cornerRadius = 12.dp.toPx()

        drawRect(color = Color.Black.copy(alpha = 0.6f))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            blendMode = BlendMode.Clear
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

/**
 * 現在の状態またはエラー内容の表示
 *
 * @param errorMessage エラーメッセージ
 * @param modifier 修飾子
 */
@Composable
private fun StatusText(errorMessage: String?, modifier: Modifier = Modifier) {
    /** 枠内のガイダンスまたはエラー内容を表示 */
    Text(
        text = errorMessage ?: "枠内にコードを合わせてください",
        color = if (errorMessage != null) MaterialTheme.colorScheme.error else Color.White,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    )
}

/**
 * 処理中の円形インジケータ
 */
@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color.White)
    }
}

/**
 * 下部のアクションボタン群
 *
 * @param isDeletePressed 削除ボタン押下フラグ
 * @param onDeletePressChange 押下状態変更コールバック
 * @param onCaptureClick スキャン実行ボタン押下時の処理
 * @param onNextClick 次へボタン押下時の処理
 * @param onEnterClick 改行ボタン押下時の処理
 * @param onOpenApp 設定アプリを開くコールバック
 * @param modifier 修飾子
 */
@Composable
private fun KeyboardControls(
    isDeletePressed: Boolean,
    onDeletePressChange: (Boolean) -> Unit,
    onCaptureClick: () -> Unit,
    onNextClick: () -> Unit,
    onEnterClick: () -> Unit,
    onOpenApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row (
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = onOpenApp,
                modifier = Modifier
                    .padding(8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.Settings, "設定", tint = Color.White)
            }

            IconButton(
                onClick = onNextClick,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f))
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardTab, "次へ", tint = Color.Black)
            }
        }

        IconButton(
            onClick = onCaptureClick,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(Icons.Default.CameraAlt, "スキャン", tint = Color.Black, modifier = Modifier.size(36.dp))
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isDeletePressed) Color.Gray.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.8f))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            onDeletePressChange(true)
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })
                            onDeletePressChange(false)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Backspace, "削除", tint = Color.Black)
            }

            IconButton(
                onClick = onEnterClick,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f))
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardReturn, "エンター", tint = Color.Black)
            }
        }
    }
}

/**
 * カメラ権限不足時のメッセージ表示
 *
 * @param onOpenSettings 設定画面起動コールバック
 * @param modifier 修飾子
 */
@Composable
private fun PermissionRequiredContent(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("カメラのアクセス権限が必要です", color = Color.White)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onOpenSettings) { Text("設定を開く") }
        }
    }
}

/**
 * 枠サイズ変更のジェスチャー解析
 *
 * 1本指スワイプまたは2本指ピンチを検知し枠サイズを更新
 *
 * @param useSwipeGesture スワイプ使用フラグ
 * @param onUpdateBox 更新通知コールバック
 */
private suspend fun PointerInputScope.handleGesture(
    useSwipeGesture: Boolean,
    onUpdateBox: (Float, Float) -> Unit
) {
    awaitEachGesture {
        awaitFirstDown()
        do {
            val event = awaitPointerEvent()
            /** ボタン等で消費されていないポインターのみを抽出 */
            val pointers = event.changes.filter { it.pressed && !it.isConsumed }
            
            if (useSwipeGesture && pointers.size == 1) {
                /** 1本指スワイプによるサイズ変更（高感度設定） */
                val change = pointers.first()
                val sensitivity = 0.001f
                val dx = change.position.x - change.previousPosition.x
                val dy = change.position.y - change.previousPosition.y
                if (abs(dx) > abs(dy)) onUpdateBox(dx * sensitivity, 0f)
                else onUpdateBox(0f, dy * sensitivity)
                change.consume()
            } else if (!useSwipeGesture && pointers.size == 2) {
                /** 2本指ピンチによるサイズ変更 */
                val p1 = pointers[0].position
                val p2 = pointers[1].position
                val pp1 = pointers[0].previousPosition
                val pp2 = pointers[1].previousPosition
                
                val dx = p1.x - p2.x
                val dy = p1.y - p2.y
                val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
                val absAngle = abs(angle)
                val normalizedAngle = if (absAngle > 90) 180 - absAngle else absAngle

                if (normalizedAngle <= 45) {
                    /** 水平方向のピンチ */
                    val currentDistX = abs(p1.x - p2.x)
                    val prevDistX = abs(pp1.x - pp2.x)
                    if (prevDistX > 0) onUpdateBox((currentDistX / prevDistX - 1f) * 0.1f, 0f)
                } else {
                    /** 垂直方向のピンチ */
                    val currentDistY = abs(p1.y - p2.y)
                    val prevDistY = abs(pp1.y - pp2.y)
                    if (prevDistY > 0) onUpdateBox(0f, (currentDistY / prevDistY - 1f) * 0.1f)
                }
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}

/**
 * デフォルト状態のプレビュー
 */
@Preview(showBackground = true, name = "Default State")
@Composable
private fun OcrKeyboardScreenNormalPreview() {
    MaterialTheme {
        OcrKeyboardContent(
            state = OcrKeyboardState(),
            onIntent = {},
            cameraPreview = { Box(it.background(Color.DarkGray)) },
            onCapture = { _, _, _, _, _, _ -> },
            onOpenApp = {}
        )
    }
}

/**
 * 候補表示状態のプレビュー
 */
@Preview(showBackground = true, name = "Suggestion State")
@Composable
private fun OcrKeyboardScreenSuggestionPreview() {
    MaterialTheme {
        OcrKeyboardContent(
            state = OcrKeyboardState(
                recognizedText = "123-456-789",
                suggestionCandidates = listOf("123456789", "123", "456", "789")
            ),
            onIntent = {},
            cameraPreview = { Box(it.background(Color.DarkGray)) },
            onCapture = { _, _, _, _, _, _ -> },
            onOpenApp = {}
        )
    }
}

/**
 * 認識処理中のプレビュー
 */
@Preview(showBackground = true, name = "Recognizing State")
@Composable
private fun OcrKeyboardScreenLoadingPreview() {
    MaterialTheme {
        OcrKeyboardContent(
            state = OcrKeyboardState(isRecognizing = true),
            onIntent = {},
            cameraPreview = { Box(it.background(Color.DarkGray)) },
            onCapture = { _, _, _, _, _, _ -> },
            onOpenApp = {}
        )
    }
}

/**
 * エラー発生時のプレビュー
 */
@Preview(showBackground = true, name = "Error State")
@Composable
private fun OcrKeyboardScreenErrorPreview() {
    MaterialTheme {
        OcrKeyboardContent(
            state = OcrKeyboardState(errorMessage = "テキストが検出されませんでした"),
            onIntent = {},
            cameraPreview = { Box(it.background(Color.DarkGray)) },
            onCapture = { _, _, _, _, _, _ -> },
            onOpenApp = {}
        )
    }
}
