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
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.KeyboardTab
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.haru.ocrkeyboard.data.local.SettingsStore
import kotlin.math.abs
import kotlin.math.atan2
import kotlinx.coroutines.delay

/**
 * OCRキーボードのメイン画面（Stateful Composable）。
 * 権限チェック、カメラ制御、設定の読み込みなどの副作用を管理します。
 *
 * @param state UI状態
 * @param onIntent インテント（アクション）発行用コールバック
 * @param modifier 修飾子
 */
@Composable
fun OcrKeyboardScreen(
    state: OcrKeyboardState,
    onIntent: (OcrKeyboardIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // ウィンドウが表示(ON_RESUME)されるたびに権限を再チェックする
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

    val imageCapture = remember { ImageCapture.Builder().build() }
    val settingsStore = remember { SettingsStore(context) }
    
    OcrKeyboardContent(
        state = state,
        onIntent = onIntent,
        useSwipeGesture = settingsStore.useSwipeGesture,
        cameraPreview = { previewModifier ->
            CameraPreview(
                imageCapture = imageCapture,
                onCameraReady = { onIntent(OcrKeyboardIntent.CameraReady) },
                modifier = previewModifier
            )
        },
        onCapture = { width, height, boxWidth, boxHeight, boxTop ->
            val executor = ContextCompat.getMainExecutor(context)
            takePicture(
                imageCapture = imageCapture,
                executor = executor,
                onCaptured = { bytes, rotation ->
                    onIntent(
                        OcrKeyboardIntent.RecognizeText(
                            bytes, rotation, width, height, boxWidth, boxHeight, boxTop
                        )
                    )
                },
                onError = { /* エラー時の通知処理（必要に応じてViewModel経由でstate更新） */ }
            )
        },
        modifier = modifier
    )
}

/**
 * OCRキーボードのUIレイアウト（Stateless Composable）。
 * UIの表示とユーザーインタラクションの検知に特化しており、プレビューが可能です。
 *
 * @param state UI状態
 * @param onIntent インテント発行用コールバック
 * @param useSwipeGesture ジェスチャー操作にスワイプを使用するか（falseの場合はピンチ）
 * @param cameraPreview カメラプレビューを表示するComposable
 * @param onCapture シャッターボタン押下時のコールバック（プレビューサイズと枠の比率を渡す）
 * @param modifier 修飾子
 */
@Composable
private fun OcrKeyboardContent(
    state: OcrKeyboardState,
    onIntent: (OcrKeyboardIntent) -> Unit,
    useSwipeGesture: Boolean,
    cameraPreview: @Composable (Modifier) -> Unit,
    onCapture: (Int, Int, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var previewWidth by remember { mutableStateOf(0) }
    var previewHeight by remember { mutableStateOf(0) }
    var boxWidthRatio by remember { mutableStateOf(0.8f) }
    var boxHeightRatio by remember { mutableStateOf(0.15f) }
    val boxTopRatio = 0.08f

    // 削除キーのオートリピート管理
    var isDeletePressed by remember { mutableStateOf(false) }
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
            .pointerInput(useSwipeGesture) {
                handleGesture(
                    useSwipeGesture = useSwipeGesture,
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

        StatusText(
            errorMessage = state.errorMessage,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (state.isRecognizing) {
            LoadingOverlay()
        } else {
            KeyboardControls(
                isDeletePressed = isDeletePressed,
                onDeletePressChange = { isDeletePressed = it },
                onCaptureClick = {
                    onCapture(previewWidth, previewHeight, boxWidthRatio, boxHeightRatio, boxTopRatio)
                },
                onNextClick = { onIntent(OcrKeyboardIntent.NextKeyPressed) },
                onEnterClick = { onIntent(OcrKeyboardIntent.EnterKeyPressed) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * 読み取り範囲を示すターゲット枠を描画するオーバーレイ。
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
 * ステータスやエラーメッセージを表示するテキストラベル。
 */
@Composable
private fun StatusText(errorMessage: String?, modifier: Modifier = Modifier) {
    Text(
        text = errorMessage ?: "枠内にコードを合わせてください",
        color = if (errorMessage != null) MaterialTheme.colorScheme.error else Color.White,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.padding(top = 10.dp)
    )
}

/**
 * 認識実行中に表示されるローディングオーバーレイ。
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
 * キーボード下部の操作ボタン群。
 */
@Composable
private fun KeyboardControls(
    isDeletePressed: Boolean,
    onDeletePressChange: (Boolean) -> Unit,
    onCaptureClick: () -> Unit,
    onNextClick: () -> Unit,
    onEnterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 削除ボタン
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isDeletePressed) Color.Gray.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.8f))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            onDeletePressChange(true)
                            waitForUpOrCancellation()
                            onDeletePressChange(false)
                        }
                    }
            ) {
                Icon(Icons.AutoMirrored.Filled.Backspace, "削除", tint = Color.Black)
            }
        }

        // シャッターボタン
        IconButton(
            onClick = onCaptureClick,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(Icons.Default.CameraAlt, "スキャン", tint = Color.Black, modifier = Modifier.size(36.dp))
        }

        // 右側機能ボタン（次へ・エンター）
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = onNextClick,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f))
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardTab, "次へ", tint = Color.Black)
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
 * カメラ権限が不足している場合に表示されるメッセージ画面。
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
 * ユーザーのポインター入力を解析し、読み取り枠のサイズを更新するためのジェスチャーハンドラ。
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.handleGesture(
    useSwipeGesture: Boolean,
    onUpdateBox: (Float, Float) -> Unit
) {
    awaitEachGesture {
        awaitFirstDown()
        do {
            val event = awaitPointerEvent()
            val pointers = event.changes.filter { it.pressed }
            if (useSwipeGesture && pointers.size == 1) {
                val change = pointers.first()
                val sensitivity = 0.001f
                val dx = change.position.x - change.previousPosition.x
                val dy = change.position.y - change.previousPosition.y
                if (abs(dx) > abs(dy)) onUpdateBox(dx * sensitivity, 0f)
                else onUpdateBox(0f, dy * sensitivity)
                change.consume()
            } else if (!useSwipeGesture && pointers.size == 2) {
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
                    val currentDistX = abs(p1.x - p2.x)
                    val prevDistX = abs(pp1.x - pp2.x)
                    if (prevDistX > 0) onUpdateBox((currentDistX / prevDistX - 1f) * 0.1f, 0f)
                } else {
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
 * OcrKeyboardScreenのプレビュー。
 * StatelessなContentを呼び出すことで、実際のカメラ動作をシミュレートせずにデザインを確認します。
 */
@Preview(showBackground = true)
@Composable
private fun OcrKeyboardScreenPreview() {
    MaterialTheme {
        Column {
            OcrKeyboardContent(
                state = OcrKeyboardState(isCameraReady = true),
                onIntent = {},
                useSwipeGesture = true,
                cameraPreview = { Box(it.background(Color.DarkGray)) },
                onCapture = { _, _, _, _, _ -> }
            )
        }
    }
}
