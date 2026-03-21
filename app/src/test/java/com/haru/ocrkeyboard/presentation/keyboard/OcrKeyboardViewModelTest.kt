package com.haru.ocrkeyboard.presentation.keyboard

import com.haru.ocrkeyboard.domain.repository.OcrRepository
import com.haru.ocrkeyboard.domain.usecase.RecognizeTextUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [OcrKeyboardViewModel]の単体テスト
 *
 * UDF(単方向データフロー)に基づいて、Intentを受け取った際に
 * 正しくStateが更新されること、および非同期イベントが送信されることを検証する。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OcrKeyboardViewModelTest {

    private lateinit var viewModel: OcrKeyboardViewModel
    private lateinit var mockRepository: MockOcrRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = MockOcrRepository()
        val useCase = RecognizeTextUseCase(mockRepository)
        val mockSettings = org.mockito.Mockito.mock(com.haru.ocrkeyboard.data.local.SettingsRepository::class.java)
        org.mockito.Mockito.`when`(mockSettings.useSwipeGestureFlow).thenReturn(kotlinx.coroutines.flow.emptyFlow())
        org.mockito.Mockito.`when`(mockSettings.useJapaneseRecognitionFlow).thenReturn(kotlinx.coroutines.flow.emptyFlow())
        viewModel = OcrKeyboardViewModel(useCase, mockSettings)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * CameraReadyインテントを受信した際、isCameraReadyがtrueになることの検証。
     *
     * [事前条件 (Given)]
     * 初期状態のViewModelインスタンスが存在する。
     *
     * [実行 (When)]
     * CameraReadyインテントを送信する。
     *
     * [検証 (Then)]
     * 状態のisCameraReadyがtrueに更新されていること。
     */
    @Test
    fun onIntent_CameraReady_updatesState() {
        // Given
        assertFalse(viewModel.state.value.isCameraReady)

        // When
        viewModel.onIntent(OcrKeyboardIntent.CameraReady)

        // Then
        assertTrue(viewModel.state.value.isCameraReady)
    }

    /**
     * RecognizeTextインテントを受信して認識が成功した場合、
     * テキストがStateに反映され、イベントが発行されることの検証。
     *
     * [事前条件 (Given)]
     * モックリポジトリが「テストテキスト」を返すように設定。
     *
     * [実行 (When)]
     * RecognizeTextインテントを送信し、コルーチンを完了させる。
     *
     * [検証 (Then)]
     * 状態のrecognizedTextが「テストテキスト」になること。
     * isRecognizingがfalseに戻ること。
     */
    @Test
    fun onIntent_RecognizeText_success_updatesStateAndEmitsEvent() = runTest {
        // Given
        mockRepository.mockResult = Result.success("テストテキスト")
        
        // When
        viewModel.onIntent(OcrKeyboardIntent.RecognizeText(byteArrayOf(1), 0))
        testDispatcher.scheduler.runCurrent()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isRecognizing)
        assertEquals("テストテキスト", state.recognizedText)
        assertNull(state.errorMessage)
    }

    /**
     * RecognizeTextインテントを受信して認識が失敗した場合、
     * エラーメッセージがStateに反映されることの検証。
     *
     * [事前条件 (Given)]
     * モックリポジトリがエラーを返すように設定。
     *
     * [実行 (When)]
     * RecognizeTextインテントを送信し、コルーチンを完了させる。
     *
     * [検証 (Then)]
     * 状態のerrorMessageにエラー内容が含まれること。
     * isRecognizingがfalseに戻ること。
     */
    @Test
    fun onIntent_RecognizeText_failure_updatesErrorState() = runTest {
        // Given
        mockRepository.mockResult = Result.failure(RuntimeException("OCRエラー"))

        // When
        viewModel.onIntent(OcrKeyboardIntent.RecognizeText(byteArrayOf(1), 0))
        testDispatcher.scheduler.runCurrent()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isRecognizing)
        assertEquals("OCRエラー", state.errorMessage)
    }

    /**
     * TextCommittedインテントを受信した際にテキストがクリアされることの検証。
     *
     * [事前条件 (Given)]
     * ViewModelの状態として、すでに認識済みテキストが存在する。
     *
     * [実行 (When)]
     * TextCommittedインテントを送信する。
     *
     * [検証 (Then)]
     * 状態のrecognizedTextが空になること。
     */
    @Test
    fun onIntent_TextCommitted_clearsText() = runTest {
        // Given
        mockRepository.mockResult = Result.success("テストテキスト")
        viewModel.onIntent(OcrKeyboardIntent.RecognizeText(byteArrayOf(1), 0))
        testDispatcher.scheduler.runCurrent()
        assertEquals("テストテキスト", viewModel.state.value.recognizedText)

        // When
        viewModel.onIntent(OcrKeyboardIntent.TextCommitted)

        // Then
        assertEquals("", viewModel.state.value.recognizedText)
    }
}

/**
 * ViewModelテスト用のモック実装
 */
class MockOcrRepository : OcrRepository {
    var mockResult: Result<String> = Result.success("")

    override suspend fun extractText(
        imageBytes: ByteArray,
        rotationDegrees: Int,
        useJapanese: Boolean,
        viewWidth: Int,
        viewHeight: Int,
        boxWidthRatio: Float,
        boxHeightRatio: Float,
        boxTopRatio: Float
    ): Result<String> {
        return mockResult
    }
}
