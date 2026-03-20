package com.haru.ocrkeyboard.domain.repository

/**
 * OCRテキスト抽出の要求定義
 *
 * 画像データからのテキスト解析・抽出インターフェース
 */
interface OcrRepository {
    /**
     * 画像データからのテキスト抽出
     *
     * @param imageBytes 画像のバイト配列データ
     * @param rotationDegrees 画像の回転角度（0, 90, 180, 270）
     * @param useJapanese 日本語認識を使用するか
     * @param viewWidth プレビューの幅
     * @param viewHeight プレビューの高さ
     * @param boxWidthRatio スキャン枠の幅比率
     * @param boxHeightRatio スキャン枠の高さ比率
     * @param boxTopRatio スキャン枠の上部オフセット比率
     * @return 抽出された文字列の結果（Result型）
     */
    suspend fun extractText(
        imageBytes: ByteArray,
        rotationDegrees: Int,
        useJapanese: Boolean = false,
        viewWidth: Int = 0,
        viewHeight: Int = 0,
        boxWidthRatio: Float = 0.8f,
        boxHeightRatio: Float = 0.15f,
        boxTopRatio: Float = 0.05f
    ): Result<String>
}
