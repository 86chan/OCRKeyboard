package com.haru.ocrkeyboard.presentation.keyboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haru.ocrkeyboard.domain.usecase.RecognizeTextUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * OCRキーボードのバックエンド状態管理
 *
 * @property recognizeTextUseCase テキスト認識ユースケース
 * @constructor 依存関係の注入
 */
class OcrKeyboardViewModel(
    private val recognizeTextUseCase: RecognizeTextUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OcrKeyboardState())
    
    /**
     * 公開用のUI状態フロー
     */
    val state: StateFlow<OcrKeyboardState> = _state.asStateFlow()

    private val _commitTextEvent = MutableSharedFlow<String>()

    /**
     * IMEへのテキスト入力イベント
     */
    val commitTextEvent: SharedFlow<String> = _commitTextEvent.asSharedFlow()

    private val _keyEvent = MutableSharedFlow<Int>()

    /**
     * IMEへのキー送出イベント（KeyCode）
     */
    val keyEvent: SharedFlow<Int> = _keyEvent.asSharedFlow()

    /**
     * ユーザー操作の処理
     *
     * @param intent ユーザーのアクション
     */
    fun onIntent(intent: OcrKeyboardIntent) {
        when (intent) {
            is OcrKeyboardIntent.CameraReady -> {
                _state.update { it.copy(isCameraReady = true) }
            }
            is OcrKeyboardIntent.RecognizeText -> {
                recognizeImage(
                    imageBytes = intent.imageBytes,
                    rotationDegrees = intent.rotationDegrees,
                    viewWidth = intent.viewWidth,
                    viewHeight = intent.viewHeight,
                    boxWidthRatio = intent.boxWidthRatio,
                    boxHeightRatio = intent.boxHeightRatio,
                    boxTopRatio = intent.boxTopRatio
                )
            }
            is OcrKeyboardIntent.DismissError -> {
                _state.update { it.copy(errorMessage = null) }
            }
            is OcrKeyboardIntent.TextCommitted -> {
                _state.update { it.copy(recognizedText = "") }
            }
            is OcrKeyboardIntent.DeleteKeyPressed -> {
                viewModelScope.launch {
                    _keyEvent.emit(android.view.KeyEvent.KEYCODE_DEL)
                }
            }
            is OcrKeyboardIntent.EnterKeyPressed -> {
                viewModelScope.launch {
                    _keyEvent.emit(android.view.KeyEvent.KEYCODE_ENTER)
                }
            }
            is OcrKeyboardIntent.NextKeyPressed -> {
                viewModelScope.launch {
                    _keyEvent.emit(android.view.KeyEvent.KEYCODE_TAB)
                }
            }
        }
    }

    /**
     * 画像の認識処理実行
     *
     * @param imageBytes 画像バイト配列
     * @param rotationDegrees 回転角度
     * @param viewWidth プレビューの幅
     * @param viewHeight プレビューの高さ
     * @param boxWidthRatio スキャン枠の幅比率
     * @param boxHeightRatio スキャン枠の高さ比率
     * @param boxTopRatio スキャン枠の上部オフセット比率
     */
    private fun recognizeImage(
        imageBytes: ByteArray,
        rotationDegrees: Int,
        viewWidth: Int,
        viewHeight: Int,
        boxWidthRatio: Float,
        boxHeightRatio: Float,
        boxTopRatio: Float
    ) {
        if (_state.value.isRecognizing) return
        
        _state.update { it.copy(isRecognizing = true, errorMessage = null) }
        
        viewModelScope.launch {
            val result = recognizeTextUseCase(
                imageBytes,
                rotationDegrees,
                viewWidth,
                viewHeight,
                boxWidthRatio,
                boxHeightRatio,
                boxTopRatio
            )
            result.onSuccess { text ->
                if (text.isNotBlank()) {
                    _state.update { it.copy(isRecognizing = false, recognizedText = text) }
                    _commitTextEvent.emit(text)
                } else {
                    _state.update { it.copy(isRecognizing = false, errorMessage = "テキストが検出されませんでした") }
                    scheduleErrorDismissal()
                }
            }
            result.onFailure { error ->
                _state.update { it.copy(isRecognizing = false, errorMessage = error.message ?: "認識中にエラーが発生しました") }
                scheduleErrorDismissal()
            }
        }
    }

    /**
     * エラーメッセージの5秒後自動クリア
     *
     * エラー表示後、一定時間でガイドテキストに戻す
     */
    private fun scheduleErrorDismissal() {
        viewModelScope.launch {
            delay(ERROR_DISMISS_DELAY_MS)
            _state.update { it.copy(errorMessage = null) }
        }
    }

    companion object {
        /** エラーメッセージの自動消去までの時間（ミリ秒） */
        private const val ERROR_DISMISS_DELAY_MS = 5_000L
    }
}
