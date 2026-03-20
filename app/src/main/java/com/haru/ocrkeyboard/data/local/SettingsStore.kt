package com.haru.ocrkeyboard.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * アプリ内設定の保存・読み出しインターフェース
 *
 * @param context アプリケーションコンテキスト
 */
class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ocr_settings", Context.MODE_PRIVATE)

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

    companion object {
        private const val PREF_USE_SWIPE_GESTURE = "use_swipe_gesture"
    }
}
