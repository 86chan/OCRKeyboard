package com.haru.ocrkeyboard.presentation.keyboard

import com.haru.ocrkeyboard.data.local.SettingsRepository
import com.haru.ocrkeyboard.domain.model.SplitDelimiter
import com.haru.ocrkeyboard.domain.usecase.RecognizeTextUseCase
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.Method
import org.mockito.Mockito

/**
 * [OcrKeyboardViewModel]の候補生成ロジック(generateCandidates)の単体テスト
 *
 * プライベートメソッドをリフレクションで呼び出し、
 * 区切り文字設定に応じた入力候補生成の境界値を検証する。
 */
class OcrKeyboardViewModelCandidateTest {

    private val useCase = Mockito.mock(RecognizeTextUseCase::class.java)
    private val settings = Mockito.mock(SettingsRepository::class.java)
    init {
        Mockito.`when`(settings.charReplacementsFlow).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))
        Mockito.`when`(settings.splitDelimitersFlow).thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))
        Mockito.`when`(settings.useSwipeGestureFlow).thenReturn(kotlinx.coroutines.flow.flowOf(false))
        Mockito.`when`(settings.useJapaneseRecognitionFlow).thenReturn(kotlinx.coroutines.flow.flowOf(false))
    }
    private val viewModel = OcrKeyboardViewModel(useCase, settings)

    private val generateCandidatesMethod: Method = OcrKeyboardViewModel::class.java.getDeclaredMethod(
        "generateCandidates",
        String::class.java,
        List::class.java
    ).apply { isAccessible = true }

    @Suppress("UNCHECKED_CAST")
    private fun invokeGenerateCandidates(text: String, delimiters: List<SplitDelimiter>): List<String> {
        return generateCandidatesMethod.invoke(viewModel, text, delimiters) as List<String>
    }

    /**
     * 有効な区切り文字が含まれる場合、正しく候補が生成されることの検証。
     *
     * [事前条件 (Given)]
     * 文字列「123-456-789」と、区切り文字「-」が有効に設定されている状態。
     *
     * [実行 (When)]
     * `generateCandidates` を呼び出す。
     *
     * [検証 (Then)]
     * 元の文字列、結合された文字列、および分割された各パーツがリストとして返されること。
     */
    @Test
    fun generateCandidates_withValidDelimiters_returnsCandidates() {
        val delimiters = listOf(SplitDelimiter("-", isEnabled = true))
        val result = invokeGenerateCandidates("123-456-789", delimiters)

        val expected = listOf("123-456-789", "123456789", "123", "456", "789")
        assertEquals(expected, result)
    }

    /**
     * 区切り文字が含まれていない場合、空のリストが返されることの検証。
     *
     * [事前条件 (Given)]
     * 区切り文字を含まない文字列と、有効な区切り文字が設定されている状態。
     *
     * [実行 (When)]
     * `generateCandidates` を呼び出す。
     *
     * [検証 (Then)]
     * 候補が生成されず、空のリストが返されること。
     */
    @Test
    fun generateCandidates_withoutDelimitersInText_returnsEmptyList() {
        val delimiters = listOf(SplitDelimiter("-", isEnabled = true))
        val result = invokeGenerateCandidates("123456789", delimiters)

        assertEquals(emptyList<String>(), result)
    }

    /**
     * 区切り文字が無効な場合、空のリストが返されることの検証。
     *
     * [事前条件 (Given)]
     * 区切り文字を含む文字列と、その区切り文字が無効に設定されている状態。
     *
     * [実行 (When)]
     * `generateCandidates` を呼び出す。
     *
     * [検証 (Then)]
     * 無効な区切り文字は無視され、空のリストが返されること。
     */
    @Test
    fun generateCandidates_withDisabledDelimiters_returnsEmptyList() {
        val delimiters = listOf(SplitDelimiter("-", isEnabled = false))
        val result = invokeGenerateCandidates("123-456-789", delimiters)

        assertEquals(emptyList<String>(), result)
    }
}
