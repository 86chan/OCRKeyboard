package com.haru.ocrkeyboard.presentation.keyboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haru.ocrkeyboard.data.local.SettingsRepository
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
 * @property settingsRepository 設定リポジトリ
 */
class OcrKeyboardViewModel(
    private val recognizeTextUseCase: RecognizeTextUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    /** 内部保持用のUI状態 */
    private val _state = MutableStateFlow(OcrKeyboardState())
    
    /** 公開用のUI状態フロー */
    val state: StateFlow<OcrKeyboardState> = _state.asStateFlow()

    /** 内部保持用のテキスト入力イベントストリーム */
    private val _commitTextEvent = MutableSharedFlow<String>()

    /** IMEへのテキスト入力イベント */
    val commitTextEvent: SharedFlow<String> = _commitTextEvent.asSharedFlow()

    /** 内部保持用のキーコード送出イベントストリーム */
    private val _keyEvent = MutableSharedFlow<Int>()

    /** IMEへのキー送出イベント（KeyCode） */
    val keyEvent: SharedFlow<Int> = _keyEvent.asSharedFlow()

    init {
        /** 設定ストリームの購読とUI状態への同期 */
        viewModelScope.launch {
            settingsRepository.useSwipeGestureFlow.collect { isEnabled ->
                _state.update { it.copy(useSwipeGesture = isEnabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.useJapaneseRecognitionFlow.collect { isEnabled ->
                _state.update { it.copy(useJapanese = isEnabled) }
            }
        }
    }

    /**
     * ユーザー操作の処理
     *
     * 画面から発行されたインテントに応じた状態更新またはイベント送出
     *
     * @param intent ユーザーのアクション
     */
    fun onIntent(intent: OcrKeyboardIntent) {
        when (intent) {
            is OcrKeyboardIntent.CameraReady -> {
                _state.update { it.copy(isCameraReady = true) }
            }
            is OcrKeyboardIntent.RecognizeText -> {
                _state.update { it.copy(suggestionCandidates = emptyList()) }
                recognizeImage(
                    imageBytes = intent.imageBytes,
                    rotationDegrees = intent.rotationDegrees,
                    useJapanese = intent.useJapanese,
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
            is OcrKeyboardIntent.SuggestionSelected -> {
                viewModelScope.launch {
                    _commitTextEvent.emit(intent.text)
                }
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
     * ユースケースを介してOCRを実行し結果を状態へ反映
     *
     * @param imageBytes 画像データ
     * @param rotationDegrees 回転角度
     * @param useJapanese 日本語認識を使用するか
     * @param viewWidth プレビュー幅
     * @param viewHeight プレビュー高さ
     * @param boxWidthRatio スキャン枠幅比率
     * @param boxHeightRatio スキャン枠高さ比率
     * @param boxTopRatio スキャン枠上部比率
     */
    private fun recognizeImage(
        imageBytes: ByteArray,
        rotationDegrees: Int,
        useJapanese: Boolean,
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
                imageBytes = imageBytes,
                rotationDegrees = rotationDegrees,
                useJapanese = useJapanese,
                viewWidth = viewWidth,
                viewHeight = viewHeight,
                boxWidthRatio = boxWidthRatio,
                boxHeightRatio = boxHeightRatio,
                boxTopRatio = boxTopRatio
            )
            result.onSuccess { text ->
                if (text.isNotBlank()) {
                    val candidates = generateCandidates(text)
                    _state.update { 
                        it.copy(
                            isRecognizing = false, 
                            recognizedText = text,
                            suggestionCandidates = candidates
                        ) 
                    }
                    if (candidates.isEmpty()) {
                        _commitTextEvent.emit(text)
                    }
                } else {
                    _state.update { it.copy(isRecognizing = false, errorMessage = "テキストが検出されませんでした") }
                    scheduleErrorDismissal()
                }
            }
            result.onFailure { error ->
                Log.e("OcrKeyboard", "Recognition failed", error)
                _state.update { it.copy(isRecognizing = false, errorMessage = error.message ?: "認識中にエラーが発生しました") }
                scheduleErrorDismissal()
            }
        }
    }

    /**
     * 入力候補の生成
     *
     * 区切り文字に基づく文字列分割および結合パターンの作成
     *
     * @param text 認識された文字列
     * @return 候補文字列リスト
     */
    private fun generateCandidates(text: String): List<String> {
        val delimiters = charArrayOf('-', ' ', '　')
        if (!text.any { it in delimiters }) return emptyList()

        val parts = text.split(*delimiters).filter { it.isNotBlank() }
        if (parts.size <= 1) return emptyList()

        val joined = parts.joinToString("")
        return listOf(joined) + parts
    }

    /**
     * エラーメッセージの自動消去予約
     */
    private fun scheduleErrorDismissal() {
        viewModelScope.launch {
            delay(ERROR_DISMISS_DELAY_MS)
            _state.update { it.copy(errorMessage = null) }
        }
    }

    companion object {
        /** エラー表示継続時間（ミリ秒） */
        private const val ERROR_DISMISS_DELAY_MS = 5_000L
    }
}
