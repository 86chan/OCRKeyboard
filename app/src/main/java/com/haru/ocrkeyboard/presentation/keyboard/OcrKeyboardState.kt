package com.haru.ocrkeyboard.presentation.keyboard

/**
 * OCRキーボードのUI状態
 *
 * @property isCameraReady カメラの準備完了状態
 * @property isRecognizing 文字認識の実行中状態
 * @property recognizedText 認識されたテキスト
 * @property errorMessage エラー発生時のメッセージ（null時はエラーなし）
 */
data class OcrKeyboardState(
    val isCameraReady: Boolean = false,
    val isRecognizing: Boolean = false,
    val recognizedText: String = "",
    val errorMessage: String? = null
)
