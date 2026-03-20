package com.haru.ocrkeyboard.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.haru.ocrkeyboard.domain.repository.OcrRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Google ML Kitを用いたOCR処理の実装
 * 
 * @constructor デフォルトコンストラクタ
 */
class OcrRepositoryImpl : OcrRepository {

    /**
     * ML Kitのテキスト認識クライアント（ラテン文字用）
     */
    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * ML Kitのテキスト認識クライアント（日本語用）
     */
    private val japaneseRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

    /**
     * 画像バイト配列からのテキスト抽出実装
     *
     * ML Kitの非同期APIのコルーチンラップによる結果返却
     *
     * @param imageBytes 画像データ
     * @param rotationDegrees 回転角度
     * @param useJapanese 日本語認識を使用するか
     * @param viewWidth プレビューの幅
     * @param viewHeight プレビューの高さ
     * @param boxWidthRatio スキャン枠の幅比率
     * @param boxHeightRatio スキャン枠の高さ比率
     * @param boxTopRatio スキャン枠の上部オフセット比率
     * @return 抽出結果
     */
    override suspend fun extractText(
        imageBytes: ByteArray, 
        rotationDegrees: Int,
        useJapanese: Boolean,
        viewWidth: Int,
        viewHeight: Int,
        boxWidthRatio: Float,
        boxHeightRatio: Float,
        boxTopRatio: Float
    ): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val originalBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (originalBitmap == null) {
                    continuation.resume(Result.failure(IllegalArgumentException("画像のデコード失敗")))
                    return@suspendCancellableCoroutine
                }
                
                // 回転を適用して正しい向きにする
                val rotatedBitmap = if (rotationDegrees != 0) {
                    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                    Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                } else {
                    originalBitmap
                }

                val cropWidth: Int
                val cropHeight: Int
                val cropX: Int
                val cropY: Int

                if (viewWidth > 0 && viewHeight > 0) {
                    val scale = maxOf(
                        viewWidth.toFloat() / rotatedBitmap.width,
                        viewHeight.toFloat() / rotatedBitmap.height
                    )
                    
                    val scaledBitmapWidth = rotatedBitmap.width * scale
                    val scaledBitmapHeight = rotatedBitmap.height * scale
                    
                    val previewLeftOnScaled = (scaledBitmapWidth - viewWidth) / 2
                    val previewTopOnScaled = (scaledBitmapHeight - viewHeight) / 2
                    
                    val boxWidthOnPreview = viewWidth * boxWidthRatio
                    val boxHeightOnPreview = viewHeight * boxHeightRatio
                    val boxLeftOnPreview = (viewWidth - boxWidthOnPreview) / 2
                    val boxTopOnPreview = viewHeight * boxTopRatio

                    val boxLeftOnScaled = previewLeftOnScaled + boxLeftOnPreview
                    val boxTopOnScaled = previewTopOnScaled + boxTopOnPreview
                    
                    cropX = (boxLeftOnScaled / scale).toInt().coerceAtLeast(0)
                    cropY = (boxTopOnScaled / scale).toInt().coerceAtLeast(0)
                    val rawCropWidth = (boxWidthOnPreview / scale).toInt()
                    val rawCropHeight = (boxHeightOnPreview / scale).toInt()
                    cropWidth = minOf(rawCropWidth, rotatedBitmap.width - cropX)
                    cropHeight = minOf(rawCropHeight, rotatedBitmap.height - cropY)
                } else {
                    cropWidth = (rotatedBitmap.width * 0.8f).toInt()
                    cropHeight = (rotatedBitmap.height * 0.4f).toInt()
                    cropX = (rotatedBitmap.width - cropWidth) / 2
                    cropY = (rotatedBitmap.height * 0.3f).toInt()
                }
                
                val croppedBitmap = Bitmap.createBitmap(rotatedBitmap, cropX, cropY, cropWidth, cropHeight)
                val image = InputImage.fromBitmap(croppedBitmap, 0)
                
                val recognizer = if (useJapanese) japaneseRecognizer else latinRecognizer

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        if (continuation.isActive) {
                            val text = visionText.text.replace("\n", " ").trim()
                            continuation.resume(Result.success(text))
                        }
                    }
                    .addOnFailureListener { e ->
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(e))
                        }
                    }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(Result.failure(e))
                }
            }
        }
    }
}
