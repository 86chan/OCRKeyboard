package com.haru.ocrkeyboard.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.haru.ocrkeyboard.domain.repository.OcrRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.abs

/**
 * Google ML KitによるOCR処理の実装
 */
class OcrRepositoryImpl : OcrRepository {

    /**
     * ラテン文字用認識クライアント
     */
    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * 日本語用認識クライアント
     */
    private val japaneseRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

    /**
     * 画像データからのテキスト抽出
     *
     * @param imageBytes 画像データ
     * @param rotationDegrees 回転角度
     * @param useJapanese 日本語認識の有無
     * @param viewWidth プレビュー幅
     * @param viewHeight プレビュー高さ
     * @param boxWidthRatio スキャン枠の幅比率
     * @param boxHeightRatio スキャン枠の高さ比率
     * @param boxTopRatio スキャン枠の上部オフセット比率
     * @return 抽出結果（Result型）
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
    ): Result<String> = withContext(Dispatchers.Default) {
        // CPU負荷の高い画像処理をバックグラウンドスレッドにオフロードし、UIスレッドのブロックを防止
        suspendCancellableCoroutine { continuation ->
            try {
                // 必要な領域のみのデコードによるメモリ使用量削減
                val decoder = BitmapRegionDecoder.newInstance(imageBytes, 0, imageBytes.size)

                val croppedBitmap = try {
                    val width = decoder.width
                    val height = decoder.height

                    if (width <= 0 || height <= 0) {
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(IllegalArgumentException("画像サイズの取得失敗")))
                        }
                        return@suspendCancellableCoroutine
                    }

                    val isRotated = rotationDegrees == 90 || rotationDegrees == 270
                    val rotatedWidth = if (isRotated) height else width
                    val rotatedHeight = if (isRotated) width else height

                    val cropRect = calculateCropRect(
                        outW = width,
                        outH = height,
                        rotW = rotatedWidth,
                        rotH = rotatedHeight,
                        rotation = rotationDegrees,
                        viewW = viewWidth,
                        viewH = viewHeight,
                        boxWR = boxWidthRatio,
                        boxHR = boxHeightRatio,
                        boxTR = boxTopRatio
                    )

                    // OCRではアルファチャンネル不要のため、RGB_565を指定しメモリ使用量を半減
                    val decodeOptions = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }

                    decoder.decodeRegion(cropRect, decodeOptions)
                        ?: throw IllegalArgumentException("画像デコード失敗")
                } finally {
                    decoder.recycle()
                }

                val image = InputImage.fromBitmap(croppedBitmap, rotationDegrees)
                val recognizer = if (useJapanese) japaneseRecognizer else latinRecognizer

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        if (continuation.isActive) {
                            val sortedText = sortVisionText(visionText)
                            continuation.resume(Result.success(sortedText))
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

    /**
     * 認識結果の読み取り順序（上から下、左から右）へのソート
     *
     * @param visionText ML Kitの認識結果
     * @return ソート済み文字列
     */
    private fun sortVisionText(visionText: Text): String {
        val allLines = visionText.textBlocks.flatMap { it.lines }
        if (allLines.isEmpty()) return ""

        val rows = mutableListOf<MutableList<Text.Line>>()

        // 垂直方向の座標による行グループ化（微細な傾きの許容）
        allLines.sortedBy { it.boundingBox?.top ?: 0 }.forEach { line ->
            val row = rows.find { existingRow ->
                val firstBox = existingRow.first().boundingBox ?: return@find false
                val lineBox = line.boundingBox ?: return@find false
                val threshold = firstBox.height() / 2
                abs(lineBox.top - firstBox.top) < threshold
            }
            if (row != null) {
                row.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }

        // 行ごとに水平方向へソートして結合
        return rows.sortedBy { it.first().boundingBox?.top ?: 0 }
            .joinToString(" ") { row ->
                row.sortBy { it.boundingBox?.left ?: 0 }
                row.joinToString(" ") { it.text }
            }.trim()
    }

    /**
     * 画像からのクロップ領域算出
     *
     * @param outW 元画像の幅
     * @param outH 元画像の高さ
     * @param rotW 回転後の幅
     * @param rotH 回転後の高さ
     * @param rotation 回転角度
     * @param viewW プレビュー幅
     * @param viewH プレビュー高さ
     * @param boxWR スキャン枠の幅比率
     * @param boxHR スキャン枠の高さ比率
     * @param boxTR スキャン枠の上部オフセット比率
     * @return クロップ領域（Rect型）
     */
    private fun calculateCropRect(
        outW: Int,
        outH: Int,
        rotW: Int,
        rotH: Int,
        rotation: Int,
        viewW: Int,
        viewH: Int,
        boxWR: Float,
        boxHR: Float,
        boxTR: Float
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

        // 回転角度に合わせた座標変換と、元画像の範囲内への制限
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