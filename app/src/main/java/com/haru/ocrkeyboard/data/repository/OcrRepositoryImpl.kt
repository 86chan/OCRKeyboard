package com.haru.ocrkeyboard.data.repository

import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.haru.ocrkeyboard.domain.repository.OcrRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs

/**
 * Google ML Kitを用いたOCR処理の実装
 */
class OcrRepositoryImpl : OcrRepository {

    /** ラテン文字用認識クライアント */
    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** 日本語用認識クライアント */
    private val japaneseRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

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
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

                if (options.outWidth <= 0 || options.outHeight <= 0) {
                    continuation.resume(Result.failure(IllegalArgumentException("画像サイズの取得失敗")))
                    return@suspendCancellableCoroutine
                }

                val isRotated = rotationDegrees == 90 || rotationDegrees == 270
                val rotatedWidth = if (isRotated) options.outHeight else options.outWidth
                val rotatedHeight = if (isRotated) options.outWidth else options.outHeight

                val cropRect = calculateCropRect(
                    options.outWidth, options.outHeight, rotatedWidth, rotatedHeight,
                    rotationDegrees, viewWidth, viewHeight, boxWidthRatio, boxHeightRatio, boxTopRatio
                )

                val decoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    BitmapRegionDecoder.newInstance(imageBytes, 0, imageBytes.size)
                } else {
                    @Suppress("DEPRECATION")
                    BitmapRegionDecoder.newInstance(imageBytes, 0, imageBytes.size, false)
                }

                val croppedBitmap = decoder.decodeRegion(cropRect, BitmapFactory.Options()) ?: throw IllegalArgumentException("デコード失敗")
                decoder.recycle()

                val image = InputImage.fromBitmap(croppedBitmap, rotationDegrees)
                val recognizer = if (useJapanese) japaneseRecognizer else latinRecognizer

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        if (continuation.isActive) {
                            val sortedText = sortVisionText(visionText)
                            Log.d("OcrRepository", "Sorted result: '$sortedText'")
                            continuation.resume(Result.success(sortedText))
                        }
                    }
                    .addOnFailureListener { e ->
                        if (continuation.isActive) continuation.resume(Result.failure(e))
                    }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * OCR結果を読み取り順序（上から下、左から右）にソート
     *
     * @param visionText 認識結果データ
     * @return ソート済みテキスト
     */
    private fun sortVisionText(visionText: Text): String {
        val allLines = visionText.textBlocks.flatMap { it.lines }
        if (allLines.isEmpty()) return ""

        // 垂直方向の座標に基づき行をグループ化（微細な傾きを許容）
        val rows = mutableListOf<MutableList<Text.Line>>()
        allLines.sortedBy { it.boundingBox?.top ?: 0 }.forEach { line ->
            val row = rows.find { existingRow ->
                val firstBox = existingRow.first().boundingBox ?: return@find false
                val lineBox = line.boundingBox ?: return@find false
                val threshold = firstBox.height() / 2
                abs(lineBox.top - firstBox.top) < threshold
            }
            if (row != null) row.add(line) else rows.add(mutableListOf(line))
        }

        // 行ごとに左から右へソートして結合
        return rows.sortedBy { it.first().boundingBox?.top ?: 0 }
            .joinToString(" ") { row ->
                row.sortBy { it.boundingBox?.left ?: 0 }
                row.joinToString(" ") { it.text }
            }.trim()
    }

    /**
     * クロップ領域の計算
     */
    private fun calculateCropRect(
        outW: Int, outH: Int, rotW: Int, rotH: Int,
        rotation: Int, viewW: Int, viewH: Int,
        boxWR: Float, boxHR: Float, boxTR: Float
    ): Rect {
        val cropX: Int
        val cropY: Int
        val cropW: Int
        val cropH: Int

        if (viewW > 0 && viewH > 0) {
            val scale = maxOf(viewW.toFloat() / rotW, viewH.toFloat() / rotH)
            val boxWOnPreview = viewW * boxWR
            val boxHOnPreview = viewH * boxHR
            val boxLOnPreview = (viewW - boxWOnPreview) / 2
            val boxTOnPreview = viewH * boxTR

            val leftOnScaled = (rotW * scale - viewW) / 2 + boxLOnPreview
            val topOnScaled = (rotH * scale - viewH) / 2 + boxTOnPreview

            cropX = (leftOnScaled / scale).toInt().coerceAtLeast(0)
            cropY = (topOnScaled / scale).toInt().coerceAtLeast(0)
            cropW = (boxWOnPreview / scale).toInt().coerceAtMost(rotW - cropX)
            cropH = (boxHOnPreview / scale).toInt().coerceAtMost(rotH - cropY)
        } else {
            cropW = (rotW * 0.8f).toInt()
            cropH = (rotH * 0.4f).toInt()
            cropX = (rotW - cropW) / 2
            cropY = (rotH * 0.3f).toInt()
        }

        return when (rotation) {
            90 -> Rect(cropY, rotW - cropX - cropW, cropY + cropH, rotW - cropX)
            180 -> Rect(rotW - cropX - cropW, rotH - cropY - cropH, rotW - cropX, rotH - cropY)
            270 -> Rect(rotH - cropY - cropH, cropX, rotH - cropY, cropX + cropW)
            else -> Rect(cropX, cropY, cropX + cropW, cropY + cropH)
        }.apply {
            left = left.coerceIn(0, outW)
            top = top.coerceIn(0, outH)
            right = right.coerceIn(0, outW)
            bottom = bottom.coerceIn(0, outH)
        }
    }
}
