package com.haru.ocrkeyboard.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStoreの拡張プロパティによるシングルトン初期化
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 設定データの永続化とリアクティブなストリームの提供
 *
 * SharedPreferencesに代わるDataStoreによる非同期・スレッドセーフなデータ管理
 *
 * @param dataStore 設定を保存するDataStoreインスタンス
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    /**
     * コンテキストからDataStoreを取得するセカンダリコンストラクタ（既存コードとの互換性用）
     *
     * @param context アプリケーションコンテキスト
     */
    constructor(context: Context) : this(context.dataStore)

    private companion object {
        val USE_SWIPE_GESTURE = booleanPreferencesKey("use_swipe_gesture")
        val USE_JAPANESE_RECOGNITION = booleanPreferencesKey("use_japanese_recognition")
    }

    /**
     * スワイプジェスチャー有効可否のストリーム
     *
     * デフォルト値はfalse（ピンチ操作）
     */
    val useSwipeGestureFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[USE_SWIPE_GESTURE] ?: false
        }

    /**
     * 日本語認識有効可否のストリーム
     *
     * デフォルト値はfalse
     */
    val useJapaneseRecognitionFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[USE_JAPANESE_RECOGNITION] ?: false
        }

    /**
     * スワイプジェスチャー設定の更新
     *
     * @param isEnabled 有効化フラグ
     */
    suspend fun setUseSwipeGesture(isEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_SWIPE_GESTURE] = isEnabled
        }
    }

    /**
     * 日本語認識設定の更新
     *
     * @param isEnabled 有効化フラグ
     */
    suspend fun setUseJapaneseRecognition(isEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_JAPANESE_RECOGNITION] = isEnabled
        }
    }
}
