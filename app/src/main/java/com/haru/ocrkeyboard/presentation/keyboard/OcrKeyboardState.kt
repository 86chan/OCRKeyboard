package com.haru.ocrkeyboard.presentation.keyboard

import com.haru.ocrkeyboard.domain.model.CharReplacement
import com.haru.ocrkeyboard.domain.model.SplitDelimiter

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
 * @property charReplacements OCR後処理の文字置換ルール一覧（設定値）
 * @property splitDelimiters 候補生成の分割に使用する区切り文字一覧（設定値）
 */
data class OcrKeyboardState(
    val isCameraReady: Boolean = false,
    val isRecognizing: Boolean = false,
    val recognizedText: String = "",
    val suggestionCandidates: List<String> = emptyList(),
    val errorMessage: String? = null,
    val useSwipeGesture: Boolean = false,
    val useJapanese: Boolean = false,
    val charReplacements: List<CharReplacement> = emptyList(),
    val splitDelimiters: List<SplitDelimiter> = emptyList(),
)
