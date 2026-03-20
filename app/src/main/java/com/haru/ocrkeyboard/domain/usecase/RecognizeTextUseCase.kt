package com.haru.ocrkeyboard.domain.usecase

import com.haru.ocrkeyboard.domain.repository.OcrRepository

/**
 * 画像からのテキスト認識実行ユースケース
 *
 * @property repository OCR処理リポジトリ
 * @constructor 依存関係の注入
 */
class RecognizeTextUseCase(
    private val repository: OcrRepository
) {

    /**
     * テキスト認識の実行
     *
     * @param imageBytes 画像バイト配列
     * @param rotationDegrees 回転角度
     * @param viewWidth プレビューの幅
     * @param viewHeight プレビューの高さ
     * @param boxWidthRatio スキャン枠の幅比率
     * @param boxHeightRatio スキャン枠の高さ比率
     * @param boxTopRatio スキャン枠の上部オフセット比率
     * @return 認識成功時は文字列、失敗時は例外を含む結果
     */
    suspend operator fun invoke(
        imageBytes: ByteArray,
        rotationDegrees: Int,
        viewWidth: Int = 0,
        viewHeight: Int = 0,
        boxWidthRatio: Float = 0.8f,
        boxHeightRatio: Float = 0.15f,
        boxTopRatio: Float = 0.05f
    ): Result<String> {
        if (imageBytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("空の画像データ"))
        }
        return repository.extractText(
            imageBytes,
            rotationDegrees,
            viewWidth,
            viewHeight,
            boxWidthRatio,
            boxHeightRatio,
            boxTopRatio
        )
    }
}
