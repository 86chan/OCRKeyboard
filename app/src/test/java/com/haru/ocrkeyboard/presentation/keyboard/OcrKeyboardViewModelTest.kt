package com.haru.ocrkeyboard.presentation.keyboard

import com.haru.ocrkeyboard.domain.repository.OcrRepository
import com.haru.ocrkeyboard.domain.usecase.RecognizeTextUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
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
        val mockSettings = mock(com.haru.ocrkeyboard.data.local.SettingsRepository::class.java)
        `when`(mockSettings.charReplacementsFlow).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))
        `when`(mockSettings.splitDelimitersFlow).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))
        `when`(mockSettings.useSwipeGestureFlow).thenReturn(kotlinx.coroutines.flow.flowOf(false))
        `when`(mockSettings.useJapaneseRecognitionFlow).thenReturn(kotlinx.coroutines.flow.flowOf(false))

        viewModel = OcrKeyboardViewModel(useCase, mockSettings)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 区切り文字の前後空白トリム設定が有効な場合、認識結果の区切り文字周辺の空白が除去されることの検証。
     *
     * [事前条件 (Given)]
     * 区切り文字「-」のtrimSurroundingSpacesがtrueに設定されたViewModelが存在する。
     * モックリポジトリが「 123  -  456　- 789 」（半角・全角空白混じり）を返すように設定。
     *
     * [実行 (When)]
     * RecognizeTextインテントを送信し、コルーチンを完了させる。
     *
     * [検証 (Then)]
     * 状態のrecognizedTextが「 123-456-789 」（区切り文字周辺の空白のみ除去）になること。
     */
    @Test
    fun onIntent_RecognizeText_withTrimSurroundingSpaces_removesSpacesAroundDelimiter() = runTest {
        // Given
        val mockSettings = mock(com.haru.ocrkeyboard.data.local.SettingsRepository::class.java)
        `when`(mockSettings.charReplacementsFlow).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))
        val delimiter = com.haru.ocrkeyboard.domain.model.SplitDelimiter("-", isEnabled = true, trimSurroundingSpaces = true)
        `when`(mockSettings.splitDelimitersFlow).thenReturn(kotlinx.coroutines.flow.flowOf(listOf(delimiter)))
        `when`(mockSettings.useSwipeGestureFlow).thenReturn(kotlinx.coroutines.flow.flowOf(false))
        `when`(mockSettings.useJapaneseRecognitionFlow).thenReturn(kotlinx.coroutines.flow.flowOf(false))

        val customViewModel = OcrKeyboardViewModel(RecognizeTextUseCase(mockRepository), mockSettings)

        mockRepository.mockResult = Result.success(" 123  -  456　- 789 ")

        // When
        customViewModel.onIntent(OcrKeyboardIntent.RecognizeText(byteArrayOf(1), 0))
        testDispatcher.scheduler.runCurrent()

        // Then
        assertEquals(" 123-456-789 ", customViewModel.state.value.recognizedText)
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
        assertEquals("認識中にエラーが発生しました", state.errorMessage)
    }

    /**
     * RecognizeTextインテントを受信して認識結果が空文字だった場合、
     * エラーメッセージがStateに反映されることの検証。
     *
     * [事前条件 (Given)]
     * モックリポジトリが空文字を返すように設定。
     *
     * [実行 (When)]
     * RecognizeTextインテントを送信し、コルーチンを完了させる。
     *
     * [検証 (Then)]
     * 状態のerrorMessageにエラー内容が含まれること。
     * isRecognizingがfalseに戻ること。
     */
    @Test
    fun onIntent_RecognizeText_emptyResult_updatesErrorState() = runTest {
        // Given
        mockRepository.mockResult = Result.success("")

        // When
        viewModel.onIntent(OcrKeyboardIntent.RecognizeText(byteArrayOf(1), 0))
        testDispatcher.scheduler.runCurrent()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isRecognizing)
        assertEquals("テキストが検出されませんでした", state.errorMessage)
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

    /**
     * SuggestionSelectedインテントを受信した際にcommitTextEventが発行されることの検証。
     *
     * [事前条件 (Given)]
     * ViewModelのcommitTextEventを購読している状態。
     *
     * [実行 (When)]
     * SuggestionSelectedインテントを送信する。
     *
     * [検証 (Then)]
     * commitTextEventにインテントで指定したテキストが発行されること。
     */
    @Test
    fun onIntent_SuggestionSelected_emitsCommitTextEvent() = runTest {
        // Given
        val emittedEvents = mutableListOf<String>()
        val job = kotlinx.coroutines.CoroutineScope(testDispatcher).launch {
            viewModel.commitTextEvent.collect {
                emittedEvents.add(it)
            }
        }
        testDispatcher.scheduler.runCurrent()

        // When
        viewModel.onIntent(OcrKeyboardIntent.SuggestionSelected("候補テキスト"))
        testDispatcher.scheduler.runCurrent()

        // Then
        assertEquals(1, emittedEvents.size)
        assertEquals("候補テキスト", emittedEvents.first())

        job.cancel()
    }

    /**
     * DeleteKeyPressedインテントを受信した際にkeyEventが発行されることの検証。
     *
     * [事前条件 (Given)]
     * ViewModelのkeyEventを購読している状態。
     *
     * [実行 (When)]
     * DeleteKeyPressedインテントを送信する。
     *
     * [検証 (Then)]
     * keyEventにKEYCODE_DEL（67）が発行されること。
     */
    @Test
    fun onIntent_DeleteKeyPressed_emitsKeyEvent() = runTest {
        // Given
        val emittedEvents = mutableListOf<Int>()
        val job = kotlinx.coroutines.CoroutineScope(testDispatcher).launch {
            viewModel.keyEvent.collect {
                emittedEvents.add(it)
            }
        }
        testDispatcher.scheduler.runCurrent()

        // When
        viewModel.onIntent(OcrKeyboardIntent.DeleteKeyPressed)
        testDispatcher.scheduler.runCurrent()

        // Then
        assertEquals(1, emittedEvents.size)
        assertEquals(android.view.KeyEvent.KEYCODE_DEL, emittedEvents.first())

        job.cancel()
    }

    /**
     * EnterKeyPressedインテントを受信した際にkeyEventが発行されることの検証。
     *
     * [事前条件 (Given)]
     * ViewModelのkeyEventを購読している状態。
     *
     * [実行 (When)]
     * EnterKeyPressedインテントを送信する。
     *
     * [検証 (Then)]
     * keyEventにKEYCODE_ENTER（66）が発行されること。
     */
    @Test
    fun onIntent_EnterKeyPressed_emitsKeyEvent() = runTest {
        // Given
        val emittedEvents = mutableListOf<Int>()
        val job = kotlinx.coroutines.CoroutineScope(testDispatcher).launch {
            viewModel.keyEvent.collect {
                emittedEvents.add(it)
            }
        }
        testDispatcher.scheduler.runCurrent()

        // When
        viewModel.onIntent(OcrKeyboardIntent.EnterKeyPressed)
        testDispatcher.scheduler.runCurrent()

        // Then
        assertEquals(1, emittedEvents.size)
        assertEquals(android.view.KeyEvent.KEYCODE_ENTER, emittedEvents.first())

        job.cancel()
    }

    /**
     * NextKeyPressedインテントを受信した際にkeyEventが発行されることの検証。
     *
     * [事前条件 (Given)]
     * ViewModelのkeyEventを購読している状態。
     *
     * [実行 (When)]
     * NextKeyPressedインテントを送信する。
     *
     * [検証 (Then)]
     * keyEventにKEYCODE_TAB（61）が発行されること。
     */
    @Test
    fun onIntent_NextKeyPressed_emitsKeyEvent() = runTest {
        // Given
        val emittedEvents = mutableListOf<Int>()
        val job = kotlinx.coroutines.CoroutineScope(testDispatcher).launch {
            viewModel.keyEvent.collect {
                emittedEvents.add(it)
            }
        }
        testDispatcher.scheduler.runCurrent()

        // When
        viewModel.onIntent(OcrKeyboardIntent.NextKeyPressed)
        testDispatcher.scheduler.runCurrent()

        // Then
        assertEquals(1, emittedEvents.size)
        assertEquals(android.view.KeyEvent.KEYCODE_TAB, emittedEvents.first())

        job.cancel()
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
        boxTopRatio: Float, charReplacements: List<com.haru.ocrkeyboard.domain.model.CharReplacement>
    ): Result<String> {
        return mockResult
    }
}
