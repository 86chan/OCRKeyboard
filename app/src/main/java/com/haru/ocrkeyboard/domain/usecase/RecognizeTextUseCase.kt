package com.haru.ocrkeyboard.domain.usecase

import com.haru.ocrkeyboard.domain.repository.OcrRepository

/**
 * テキスト認識実行ユースケース
 *
 * ドメイン層におけるOCR処理の抽象化を担当
 *
 * @property ocrRepository OCRリポジトリ
 */
class RecognizeTextUseCase(
    private val ocrRepository: OcrRepository
) {
    /**
     * テキスト認識の実行
     *
     * 与えられた画像データからスキャン枠に基づいたテキスト抽出を行う
     *
     * @param imageBytes 画像データ
     * @param rotationDegrees 回転角度
     * @param useJapanese 日本語認識を使用するか
     * @param viewWidth プレビュー幅
     * @param viewHeight プレビュー高さ
     * @param boxWidthRatio スキャン枠幅比率
     * @param boxHeightRatio スキャン枠高さ比率
     * @param boxTopRatio スキャン枠上部比率
     * @return 認識結果のResult型文字列
     */
    suspend operator fun invoke(
        imageBytes: ByteArray,
        rotationDegrees: Int,
        useJapanese: Boolean = false,
        viewWidth: Int = 0,
        viewHeight: Int = 0,
        boxWidthRatio: Float = 0.8f,
        boxHeightRatio: Float = 0.15f,
        boxTopRatio: Float = 0.05f
    ): Result<String> {
        return ocrRepository.extractText(
            imageBytes,
            rotationDegrees,
            useJapanese,
            viewWidth,
            viewHeight,
            boxWidthRatio,
            boxHeightRatio,
            boxTopRatio
        )
    }
}
