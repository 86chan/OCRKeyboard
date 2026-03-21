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
                
                // 不要なBitmap生成を防ぐため、元の画像のままクロップ位置を計算する
                // Get dimensions as if it were rotated to the correct orientation
                val isRotated = rotationDegrees == 90 || rotationDegrees == 270
                val rotatedWidth = if (isRotated) originalBitmap.height else originalBitmap.width
                val rotatedHeight = if (isRotated) originalBitmap.width else originalBitmap.height

                val cropWidthRotated: Int
                val cropHeightRotated: Int
                val cropXRotated: Int
                val cropYRotated: Int

                if (viewWidth > 0 && viewHeight > 0) {
                    val scale = maxOf(
                        viewWidth.toFloat() / rotatedWidth,
                        viewHeight.toFloat() / rotatedHeight
                    )
                    
                    val scaledBitmapWidth = rotatedWidth * scale
                    val scaledBitmapHeight = rotatedHeight * scale
                    
                    val previewLeftOnScaled = (scaledBitmapWidth - viewWidth) / 2
                    val previewTopOnScaled = (scaledBitmapHeight - viewHeight) / 2
                    
                    val boxWidthOnPreview = viewWidth * boxWidthRatio
                    val boxHeightOnPreview = viewHeight * boxHeightRatio
                    val boxLeftOnPreview = (viewWidth - boxWidthOnPreview) / 2
                    val boxTopOnPreview = viewHeight * boxTopRatio

                    val boxLeftOnScaled = previewLeftOnScaled + boxLeftOnPreview
                    val boxTopOnScaled = previewTopOnScaled + boxTopOnPreview
                    
                    cropXRotated = (boxLeftOnScaled / scale).toInt().coerceAtLeast(0)
                    cropYRotated = (boxTopOnScaled / scale).toInt().coerceAtLeast(0)
                    val rawCropWidth = (boxWidthOnPreview / scale).toInt()
                    val rawCropHeight = (boxHeightOnPreview / scale).toInt()
                    cropWidthRotated = minOf(rawCropWidth, rotatedWidth - cropXRotated)
                    cropHeightRotated = minOf(rawCropHeight, rotatedHeight - cropYRotated)
                } else {
                    cropWidthRotated = (rotatedWidth * 0.8f).toInt()
                    cropHeightRotated = (rotatedHeight * 0.4f).toInt()
                    cropXRotated = (rotatedWidth - cropWidthRotated) / 2
                    cropYRotated = (rotatedHeight * 0.3f).toInt()
                }

                val origX: Int
                val origY: Int
                when (rotationDegrees) {
                    90 -> {
                        origX = cropYRotated
                        origY = rotatedWidth - cropXRotated - cropWidthRotated
                    }
                    180 -> {
                        origX = rotatedWidth - cropXRotated - cropWidthRotated
                        origY = rotatedHeight - cropYRotated - cropHeightRotated
                    }
                    270 -> {
                        origX = rotatedHeight - cropYRotated - cropHeightRotated
                        origY = cropXRotated
                    }
                    else -> {
                        origX = cropXRotated
                        origY = cropYRotated
                    }
                }
                
                val origW = if (isRotated) cropHeightRotated else cropWidthRotated
                val origH = if (isRotated) cropWidthRotated else cropHeightRotated

                val finalOrigX = origX.coerceAtLeast(0)
                val finalOrigY = origY.coerceAtLeast(0)
                val finalOrigW = origW.coerceAtMost(originalBitmap.width - finalOrigX)
                val finalOrigH = origH.coerceAtMost(originalBitmap.height - finalOrigY)

                val croppedBitmap = Bitmap.createBitmap(originalBitmap, finalOrigX, finalOrigY, finalOrigW, finalOrigH)
                val image = InputImage.fromBitmap(croppedBitmap, rotationDegrees)
                
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
