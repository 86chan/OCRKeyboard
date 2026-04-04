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

/**
 * [SettingsRepository]の単体テスト
 *
 * ローカルファイルシステム上のテスト用DataStoreを作成し、
 * 各設定項目の更新とフローの放射が正しく行われることを検証する。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    /** テストごとに破棄される一時ディレクトリルール */
    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepository
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

        repository = SettingsRepository(testDataStore)
    }

    /**
     * 日本語認識設定を更新し、フローから正しい値が読み取れることの検証。
     *
     * [事前条件 (Given)]
     * 初回実行時のため、DataStoreに設定値が未保存であり、
     * [SettingsRepository.useJapaneseRecognitionFlow] の初期値がfalseである状態。
     *
     * [実行 (When)]
     * [SettingsRepository.setUseJapaneseRecognition] を引数trueで呼び出す。
     *
     * [検証 (Then)]
     * [SettingsRepository.useJapaneseRecognitionFlow] から取得される値がtrueに更新されていること。
     */
    @Test
    fun setUseJapaneseRecognition_updatesValueAndFlowEmitsCorrectly() = runTest {
        // Given
        val initialValue = repository.useJapaneseRecognitionFlow.first()
        assertEquals("初期状態はfalseであるべき", false, initialValue)

        // When
        repository.setUseJapaneseRecognition(true)

        // Then
        val updatedValue = repository.useJapaneseRecognitionFlow.first()
        assertEquals("更新後はtrueであるべき", true, updatedValue)
    }

    /**
     * スワイプジェスチャー設定を更新し、フローから正しい値が読み取れることの検証。
     *
     * [事前条件 (Given)]
     * 初回実行時のため、DataStoreに設定値が未保存であり、
     * [SettingsRepository.useSwipeGestureFlow] の初期値がfalseである状態。
     *
     * [実行 (When)]
     * [SettingsRepository.setUseSwipeGesture] を引数trueで呼び出す。
     *
     * [検証 (Then)]
     * [SettingsRepository.useSwipeGestureFlow] から取得される値がtrueに更新されていること。
     */
    @Test
    fun setUseSwipeGesture_updatesValueAndFlowEmitsCorrectly() = runTest {
        // Given
        val initialValue = repository.useSwipeGestureFlow.first()
        assertEquals("初期状態はfalseであるべき", false, initialValue)

        // When
        repository.setUseSwipeGesture(true)

        // Then
        val updatedValue = repository.useSwipeGestureFlow.first()
        assertEquals("更新後はtrueであるべき", true, updatedValue)
    }
}
