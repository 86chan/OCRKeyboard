package com.haru.ocrkeyboard.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * SettingsRepositoryの振る舞いを検証するテストクラス
 *
 * Robolectricを使用してContextを提供し、DataStoreの動作をシミュレート
 */
@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository

    /**
     * 各テスト前の初期化処理
     *
     * ApplicationProviderからContextを取得し、SettingsRepositoryを初期化
     */
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsRepository = SettingsRepository(context)
    }

    /**
     * 初期状態の検証
     *
     * スワイプジェスチャーと日本語認識の初期値がfalseであることを確認
     */
    @Test
    fun defaultValuesAreFalse() = runTest {
        // 各設定値のFlowから最初の要素を取得
        val useSwipe = settingsRepository.useSwipeGestureFlow.first()
        val useJapanese = settingsRepository.useJapaneseRecognitionFlow.first()

        // 初期値がfalseであることをアサート
        assertEquals(false, useSwipe)
        assertEquals(false, useJapanese)
    }

    /**
     * スワイプジェスチャー設定更新の検証
     *
     * trueおよびfalseを設定した際に正しく値が更新されることを確認
     */
    @Test
    fun setUseSwipeGesture_updatesValue() = runTest {
        // trueに更新
        settingsRepository.setUseSwipeGesture(true)
        val useSwipe = settingsRepository.useSwipeGestureFlow.first()
        assertEquals(true, useSwipe)

        // falseに更新
        settingsRepository.setUseSwipeGesture(false)
        val useSwipeFalse = settingsRepository.useSwipeGestureFlow.first()
        assertEquals(false, useSwipeFalse)
    }

    /**
     * 日本語認識設定更新の検証
     *
     * trueおよびfalseを設定した際に正しく値が更新されることを確認
     */
    @Test
    fun setUseJapaneseRecognition_updatesValue() = runTest {
        // trueに更新
        settingsRepository.setUseJapaneseRecognition(true)
        val useJapanese = settingsRepository.useJapaneseRecognitionFlow.first()
        assertEquals(true, useJapanese)

        // falseに更新
        settingsRepository.setUseJapaneseRecognition(false)
        val useJapaneseFalse = settingsRepository.useJapaneseRecognitionFlow.first()
        assertEquals(false, useJapaneseFalse)
    }
}
