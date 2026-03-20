package com.haru.ocrkeyboard.presentation.keyboard.components

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.nio.ByteBuffer

/**
 * カメラプレビューと撮影機能を提供するコンポーザブル
 *
 * @param imageCapture 撮影用ユースケースのインスタンス
 * @param onCameraReady カメラ準備完了時のコールバック
 * @param modifier 修飾子
 */
@Composable
fun CameraPreview(
    imageCapture: ImageCapture,
    onCameraReady: () -> Unit,
    modifier: Modifier = Modifier
) {
    LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    onCameraReady()
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "カメラのバインドに失敗", exc)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * 画像撮影の実行
 *
 * @param imageCapture 撮影用ユースケース
 * @param executor 実行エグゼキュータ
 * @param onCaptured 撮影成功時のコールバック（バイト配列と回転角度）
 * @param onError 撮影失敗時のコールバック
 */
fun takePicture(
    imageCapture: ImageCapture,
    executor: java.util.concurrent.Executor,
    onCaptured: (ByteArray, Int) -> Unit,
    onError: (Exception) -> Unit
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val buffer: ByteBuffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.capacity())
                    buffer.get(bytes)
                    onCaptured(bytes, image.imageInfo.rotationDegrees)
                } catch (e: Exception) {
                    onError(e)
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}
