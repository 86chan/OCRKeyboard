package com.haru.ocrkeyboard.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.haru.ocrkeyboard.domain.model.CharReplacement
import com.haru.ocrkeyboard.domain.model.SplitDelimiter
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
 * @param context アプリケーションコンテキスト
 */
class SettingsRepository(private val context: Context) {

    private companion object {
        /** スワイプジェスチャー設定キー */
        val USE_SWIPE_GESTURE = booleanPreferencesKey("use_swipe_gesture")

        /** 日本語認識設定キー */
        val USE_JAPANESE_RECOGNITION = booleanPreferencesKey("use_japanese_recognition")

        /** 文字置換ルール一覧の保存キー */
        val CHAR_REPLACEMENTS = stringPreferencesKey("char_replacements")

        /** 区切り文字ルール一覧の保存キー */
        val SPLIT_DELIMITERS = stringPreferencesKey("split_delimiters")

        /**
         * デフォルトの置換ルール
         *
         * スラッシュドゼロ（Ø/ø）のOCR誤認識への初期対策
         */
        val DEFAULT_REPLACEMENTS = listOf(
            CharReplacement(from = "Ø", to = "0", isEnabled = true),
            CharReplacement(from = "ø", to = "0", isEnabled = true),
        )

        /**
         * デフォルトの区切り文字ルール
         */
        val DEFAULT_SPLIT_DELIMITERS = listOf(
            SplitDelimiter(char = "-", isEnabled = true, trimSurroundingSpaces = true),
            SplitDelimiter(char = " ", isEnabled = true, trimSurroundingSpaces = false),
            SplitDelimiter(char = "　", isEnabled = true, trimSurroundingSpaces = false),
        )

        /**
         * `List<CharReplacement>` からDataStore保存用文字列へのエンコード
         *
         * フォーマット: `"1|Ø|0\n0|ø|0"` (isEnabled|from|to、改行区切り)
         *
         * @param list 置換ルール一覧
         * @return エンコード済み文字列
         */
        fun encode(list: List<CharReplacement>): String = list.joinToString("\n") { rule ->
            val enabled = if (rule.isEnabled) "1" else "0"
            "$enabled|${rule.from}|${rule.to}"
        }

        /**
         * DataStore保存用文字列から `List<CharReplacement>` へのデコード
         *
         * パース失敗したレコードは無視することで、データ破損時の耐障害性を確保する
         *
         * @param raw エンコード済み文字列
         * @return 置換ルール一覧
         */
        fun decode(raw: String): List<CharReplacement> = raw
            .split("\n")
            .filter { it.isNotBlank() }
            .mapNotNull { record ->
                // パース失敗は警告なしでスキップ（データ破損への耐性）
                val parts = record.split("|", limit = 3)
                if (parts.size != 3) return@mapNotNull null
                val (enabledStr, from, to) = parts
                CharReplacement(
                    from = from,
                    to = to,
                    isEnabled = enabledStr == "1",
                )
            }

        /**
         * `List<SplitDelimiter>` からDataStore保存用文字列へのエンコード
         *
         * フォーマット: `"1|1|-"` (isEnabled|trimSurroundingSpaces|char、改行区切り)
         *
         * @param list 区切り文字ルール一覧
         * @return エンコード済み文字列
         */
        fun encodeSplitDelimiters(list: List<SplitDelimiter>): String = list.joinToString("\n") { rule ->
            val enabled = if (rule.isEnabled) "1" else "0"
            val trimSpaces = if (rule.trimSurroundingSpaces) "1" else "0"
            "$enabled|$trimSpaces|${rule.char}"
        }

        /**
         * DataStore保存用文字列から `List<SplitDelimiter>` へのデコード
         *
         * @param raw エンコード済み文字列
         * @return 区切り文字ルール一覧
         */
        fun decodeSplitDelimiters(raw: String): List<SplitDelimiter> = raw
            .split("\n")
            .filter { it.isNotBlank() }
            .mapNotNull { record ->
                val parts = record.split("|", limit = 3)
                if (parts.size != 3) return@mapNotNull null
                val (enabledStr, trimStr, char) = parts
                SplitDelimiter(
                    char = char,
                    isEnabled = enabledStr == "1",
                    trimSurroundingSpaces = trimStr == "1",
                )
            }
    }

    /**
     * スワイプジェスチャー有効可否のストリーム
     *
     * デフォルト値はfalse（ピンチ操作）
     */
    val useSwipeGestureFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[USE_SWIPE_GESTURE] ?: false
        }

    /**
     * 日本語認識有効可否のストリーム
     *
     * デフォルト値はfalse
     */
    val useJapaneseRecognitionFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[USE_JAPANESE_RECOGNITION] ?: false
        }

    /**
     * 文字置換ルール一覧のストリーム
     *
     * 未設定時はデフォルトの置換ルール（Ø→0, ø→0）を返す
     */
    val charReplacementsFlow: Flow<List<CharReplacement>> = context.dataStore.data
        .map { preferences ->
            preferences[CHAR_REPLACEMENTS]
                ?.let { decode(it) }
                ?: DEFAULT_REPLACEMENTS
        }

    /**
     * 区切り文字ルール一覧のストリーム
     *
     * 未設定時はデフォルトのルール（-・半角スペース・全角スペース）を返す
     */
    val splitDelimitersFlow: Flow<List<SplitDelimiter>> = context.dataStore.data
        .map { preferences ->
            preferences[SPLIT_DELIMITERS]
                ?.let { decodeSplitDelimiters(it) }
                ?: DEFAULT_SPLIT_DELIMITERS
        }

    /**
     * スワイプジェスチャー設定の更新
     *
     * @param isEnabled 有効化フラグ
     */
    suspend fun setUseSwipeGesture(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_SWIPE_GESTURE] = isEnabled
        }
    }

    /**
     * 日本語認識設定の更新
     *
     * @param isEnabled 有効化フラグ
     */
    suspend fun setUseJapaneseRecognition(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_JAPANESE_RECOGNITION] = isEnabled
        }
    }

    /**
     * 文字置換ルール一覧の更新
     *
     * @param list 更新後の置換ルール一覧
     */
    suspend fun setCharReplacements(list: List<CharReplacement>) {
        context.dataStore.edit { preferences ->
            preferences[CHAR_REPLACEMENTS] = encode(list)
        }
    }

    /**
     * 区切り文字ルール一覧の更新
     *
     * @param list 更新後の区切り文字ルール一覧
     */
    suspend fun setSplitDelimiters(list: List<SplitDelimiter>) {
        context.dataStore.edit { preferences ->
            preferences[SPLIT_DELIMITERS] = encodeSplitDelimiters(list)
        }
    }
}
