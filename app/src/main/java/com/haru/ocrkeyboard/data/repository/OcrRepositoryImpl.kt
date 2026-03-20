package com.haru.ocrkeyboard.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
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
     * ML Kitの日本語テキスト認識クライアント
     */
    private val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

    /**
     * 画像バイト配列からのテキスト抽出実装
     *
     * ML Kitの非同期APIのコルーチンラップによる結果返却
     *
     * @param imageBytes 画像データ
     * @param rotationDegrees 回転角度
     * @return 抽出結果
     */
    override suspend fun extractText(
        imageBytes: ByteArray, 
        rotationDegrees: Int,
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
                    // PreviewUses FILL_CENTER, meaning it scales the image to fill the view, 
                    // cropping the overflow on either X or Y axis depending on the aspect ratio.
                    val scale = maxOf(
                        viewWidth.toFloat() / rotatedBitmap.width,
                        viewHeight.toFloat() / rotatedBitmap.height
                    )
                    
                    val scaledBitmapWidth = rotatedBitmap.width * scale
                    val scaledBitmapHeight = rotatedBitmap.height * scale
                    
                    // The view is centered on the scaled bitmap
                    val previewLeftOnScaled = (scaledBitmapWidth - viewWidth) / 2
                    val previewTopOnScaled = (scaledBitmapHeight - viewHeight) / 2
                    
                    // The target box in the UI view
                    val boxWidthOnPreview = viewWidth * boxWidthRatio
                    val boxHeightOnPreview = viewHeight * boxHeightRatio
                    val boxLeftOnPreview = (viewWidth - boxWidthOnPreview) / 2
                    val boxTopOnPreview = viewHeight * boxTopRatio // 枠の位置

                    // Map box coordinates from Preview back to the Scaled Bitmap
                    val boxLeftOnScaled = previewLeftOnScaled + boxLeftOnPreview
                    val boxTopOnScaled = previewTopOnScaled + boxTopOnPreview
                    
                    // Map from Scaled Bitmap back to Original Bitmap
                    cropX = (boxLeftOnScaled / scale).toInt().coerceAtLeast(0)
                    cropY = (boxTopOnScaled / scale).toInt().coerceAtLeast(0)
                    val rawCropWidth = (boxWidthOnPreview / scale).toInt()
                    val rawCropHeight = (boxHeightOnPreview / scale).toInt()
                    cropWidth = minOf(rawCropWidth, rotatedBitmap.width - cropX)
                    cropHeight = minOf(rawCropHeight, rotatedBitmap.height - cropY)
                } else {
                    // Fallback using old generic percentages
                    cropWidth = (rotatedBitmap.width * 0.8f).toInt()
                    cropHeight = (rotatedBitmap.height * 0.4f).toInt()
                    cropX = (rotatedBitmap.width - cropWidth) / 2
                    cropY = (rotatedBitmap.height * 0.3f).toInt()
                }
                
                val croppedBitmap = Bitmap.createBitmap(rotatedBitmap, cropX, cropY, cropWidth, cropHeight)
                
                // InputImageには回転済み・クロップ済みのBitmapを渡すためrotationは0
                val image = InputImage.fromBitmap(croppedBitmap, 0)
                
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        if (continuation.isActive) {
                            // 改行をスペースに変換して1行のコードとして扱いやすくする
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
