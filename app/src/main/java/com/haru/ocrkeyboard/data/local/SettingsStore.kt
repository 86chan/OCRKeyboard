package com.haru.ocrkeyboard.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * アプリ内設定の保存・読み出しインターフェース
 *
 * SharedPreferencesの変更を監視し、Flowとして外部へ公開する
 *
 * @param context アプリケーションコンテキスト
 */
class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ocr_settings", Context.MODE_PRIVATE)

    private val _useSwipeGesture = MutableStateFlow(prefs.getBoolean(PREF_USE_SWIPE_GESTURE, false))
    /** リサイズ用ジェスチャー設定の監視用Flow */
    val useSwipeGestureFlow: StateFlow<Boolean> = _useSwipeGesture.asStateFlow()

    private val _useJapaneseRecognition = MutableStateFlow(prefs.getBoolean(PREF_USE_JAPANESE_RECOGNITION, false))
    /** 日本語文字認識設定の監視用Flow */
    val useJapaneseRecognitionFlow: StateFlow<Boolean> = _useJapaneseRecognition.asStateFlow()

    /** 
     * SharedPreferencesの変更リスナー
     * 強参照を保持しないとGCされるため、プロパティとして保持する
     */
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
        when (key) {
            PREF_USE_SWIPE_GESTURE -> _useSwipeGesture.value = p.getBoolean(key, false)
            PREF_USE_JAPANESE_RECOGNITION -> _useJapaneseRecognition.value = p.getBoolean(key, false)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * リサイズ用ジェスチャーがスワイプかどうかを保持するプロパティ
     *
     * true: スワイプ操作 (Swipe)
     * false: ピンチ操作 (Pinch)
     */
    var useSwipeGesture: Boolean
        get() = prefs.getBoolean(PREF_USE_SWIPE_GESTURE, false)
        set(value) {
            prefs.edit().putBoolean(PREF_USE_SWIPE_GESTURE, value).apply()
        }

    /**
     * 日本語の文字認識を使用するかどうかを保持するプロパティ
     *
     * true: 日本語認識を使用
     * false: 英数字（デフォルト）認識を使用
     */
    var useJapaneseRecognition: Boolean
        get() = prefs.getBoolean(PREF_USE_JAPANESE_RECOGNITION, false)
        set(value) {
            prefs.edit().putBoolean(PREF_USE_JAPANESE_RECOGNITION, value).apply()
        }

    companion object {
        private const val PREF_USE_SWIPE_GESTURE = "use_swipe_gesture"
        private const val PREF_USE_JAPANESE_RECOGNITION = "use_japanese_recognition"
    }
}
