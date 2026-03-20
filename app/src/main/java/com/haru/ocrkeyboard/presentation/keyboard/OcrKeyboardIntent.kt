package com.haru.ocrkeyboard.presentation.keyboard

/**
 * OCRキーボードに対するユーザーの操作
 */
sealed class OcrKeyboardIntent {
    /**
     * カメラの準備完了通知
     */
    data object CameraReady : OcrKeyboardIntent()

    /**
     * テキスト認識要求
     *
     * @property imageBytes キャプチャされた画像データ
     * @property rotationDegrees 画像の回転角度
     * @property viewWidth カメラプレビューのUI上の幅
     * @property viewHeight カメラプレビューのUI上の高さ
     * @property boxWidthRatio スキャン枠の幅比率
     * @property boxHeightRatio スキャン枠の高さ比率
     * @property boxTopRatio スキャン枠の上部オフセット比率
     */
    data class RecognizeText(
        val imageBytes: ByteArray, 
        val rotationDegrees: Int,
        val viewWidth: Int = 0,
        val viewHeight: Int = 0,
        val boxWidthRatio: Float = 0.8f,
        val boxHeightRatio: Float = 0.15f,
        val boxTopRatio: Float = 0.05f
    ) : OcrKeyboardIntent()
    
    /**
     * エラーダイアログの破棄要求
     */
    data object DismissError : OcrKeyboardIntent()

    /**
     * テキスト入力完了通知
     */
    data object TextCommitted : OcrKeyboardIntent()
    
    /**
     * 削除キーの押下
     */
    data object DeleteKeyPressed : OcrKeyboardIntent()
    
    /**
     * エンターキーの押下
     */
    data object EnterKeyPressed : OcrKeyboardIntent()
    
    /**
     * 次へ（タブ/Next）キーの押下
     */
    data object NextKeyPressed : OcrKeyboardIntent()
}
