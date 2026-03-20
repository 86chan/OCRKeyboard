package com.haru.ocrkeyboard.presentation.keyboard

/**
 * OCRキーボードのUI状態
 *
 * @property isCameraReady カメラの準備完了状態
 * @property isRecognizing 文字認識の実行中状態
 * @property recognizedText 認識されたテキスト
 * @property suggestionCandidates 入力候補リスト（シリアルコード分割用）
 * @property errorMessage エラーメッセージ（null時はエラーなし）
 * @property useSwipeGesture リサイズにスワイプを使用するか（設定値）
 * @property useJapanese 日本語文字認識を使用するか（設定値）
 */
data class OcrKeyboardState(
    val isCameraReady: Boolean = false,
    val isRecognizing: Boolean = false,
    val recognizedText: String = "",
    val suggestionCandidates: List<String> = emptyList(),
    val errorMessage: String? = null,
    val useSwipeGesture: Boolean = false,
    val useJapanese: Boolean = false
)
