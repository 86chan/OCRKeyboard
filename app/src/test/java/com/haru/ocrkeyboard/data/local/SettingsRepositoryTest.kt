package com.haru.ocrkeyboard.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * [SettingsRepository]の単体テスト
 *
 * ローカルファイルシステム上のテスト用DataStoreを作成し、
 * Robolectricを回避しつつ各設定項目の更新とフローの放射が正しく行われることを検証する。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    /** テストごとに破棄される一時ディレクトリルール */
    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var settingsRepository: SettingsRepository
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    /**
     * テストごとの初期化処理
     *
     * 一時ファイルを利用するテスト用のDataStoreを作成し、リポジトリに注入する。
     */
    @Before
    fun setUp() {
        val testFile = tmpFolder.newFile("test_settings.preferences_pb")

        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )

        settingsRepository = SettingsRepository(testDataStore)
    }

    /**
     * 初期状態の検証
     *
     * [事前条件 (Given)]
     * DataStoreが新規作成された状態。
     *
     * [実行 (When)]
     * 各フローの最初の値を取得する。
     *
     * [検証 (Then)]
     * スワイプジェスチャーと日本語認識の初期値がfalseであること。
     */
    @Test
    fun defaultValuesAreFalse() = runTest {
        // When
        val useSwipe = settingsRepository.useSwipeGestureFlow.first()
        val useJapanese = settingsRepository.useJapaneseRecognitionFlow.first()

        // Then
        assertEquals(false, useSwipe)
        assertEquals(false, useJapanese)
    }

    /**
     * スワイプジェスチャー設定更新の検証
     *
     * [事前条件 (Given)]
     * DataStoreが初期化された状態。
     *
     * [実行 (When)]
     * [SettingsRepository.setUseSwipeGesture] をtrue、その後falseで呼び出す。
     *
     * [検証 (Then)]
     * それぞれの呼び出し後に [SettingsRepository.useSwipeGestureFlow] から正しい値が放出されること。
     */
    @Test
    fun setUseSwipeGesture_updatesValue() = runTest {
        // When: trueに更新
        settingsRepository.setUseSwipeGesture(true)
        val useSwipe = settingsRepository.useSwipeGestureFlow.first()
        // Then: trueであること
        assertEquals(true, useSwipe)

        // When: falseに更新
        settingsRepository.setUseSwipeGesture(false)
        val useSwipeFalse = settingsRepository.useSwipeGestureFlow.first()
        // Then: falseであること
        assertEquals(false, useSwipeFalse)
    }

    /**
     * 日本語認識設定更新の検証
     *
     * [事前条件 (Given)]
     * DataStoreが初期化された状態。
     *
     * [実行 (When)]
     * [SettingsRepository.setUseJapaneseRecognition] をtrue、その後falseで呼び出す。
     *
     * [検証 (Then)]
     * それぞれの呼び出し後に [SettingsRepository.useJapaneseRecognitionFlow] から正しい値が放出されること。
     */
    @Test
    fun setUseJapaneseRecognition_updatesValue() = runTest {
        // When: trueに更新
        settingsRepository.setUseJapaneseRecognition(true)
        val useJapanese = settingsRepository.useJapaneseRecognitionFlow.first()
        // Then: trueであること
        assertEquals(true, useJapanese)

        // When: falseに更新
        settingsRepository.setUseJapaneseRecognition(false)
        val useJapaneseFalse = settingsRepository.useJapaneseRecognitionFlow.first()
        // Then: falseであること
        assertEquals(false, useJapaneseFalse)
    }
}
