package com.haru.ocrkeyboard.domain.usecase

import com.haru.ocrkeyboard.domain.repository.OcrRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [RecognizeTextUseCase]の単体テスト
 *
 * ユースケースがリポジトリの出力を正しくラップし、
 * 空データなどの境界値を正しく処理するかを検証する。
 */
class RecognizeTextUseCaseTest {

    private lateinit var useCase: RecognizeTextUseCase
    private lateinit var mockRepository: MockOcrRepository

    @Before
    fun setUp() {
        mockRepository = MockOcrRepository()
        useCase = RecognizeTextUseCase(mockRepository)
    }

    /**
     * 正常な画像データが渡された場合、リポジトリの成功結果がそのまま返されることの検証。
     *
     * [事前条件 (Given)]
     * モックリポジトリが「認識成功文字列」を返すよう設定されている状態。
     *
     * [実行 (When)]
     * RecognizeTextUseCaseを、有効な画像バイト配列と回転角度を指定して呼び出す。
     *
     * [検証 (Then)]
     * Result.isSuccessがtrueであり、取得されたテキストが「認識成功文字列」であること。
     */
    @Test
    fun invoke_withValidImage_returnsSuccess() = runTest {
        // Given
        val validBytes = byteArrayOf(1, 2, 3)
        mockRepository.mockResult = Result.success("認識成功文字列")

        // When
        val result = useCase(validBytes, 0)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("認識成功文字列", result.getOrNull())
    }

    /**
     * 空の画像データが渡された場合、即座にIllegalArgumentExceptionで失敗することの検証。
     *
     * [事前条件 (Given)]
     * 特になし。
     *
     * [実行 (When)]
     * RecognizeTextUseCaseを、空のバイト配列を指定して呼び出す。
     *
     * [検証 (Then)]
     * Result.isFailureがtrueであり、例外がIllegalArgumentExceptionであること。
     * モックリポジトリが呼び出されていないこと。
     */
    @Test
    fun invoke_withEmptyImage_returnsFailure() = runTest {
        // Given
        val emptyBytes = byteArrayOf()

        // When
        val result = useCase(emptyBytes, 0)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(0, mockRepository.callCount)
    }

    /**
     * リポジトリでエラーが発生した場合、そのエラーが正しく伝播されることの検証。
     *
     * [事前条件 (Given)]
     * モックリポジトリが任意の例外を返すよう設定されている状態。
     *
     * [実行 (When)]
     * RecognizeTextUseCaseを呼び出す。
     *
     * [検証 (Then)]
     * Result.isFailureがtrueであり、モックが投げた例外と同じ例外オブジェクトが返されること。
     */
    @Test
    fun invoke_whenRepositoryFails_returnsFailure() = runTest {
        // Given
        val validBytes = byteArrayOf(1)
        val exception = RuntimeException("OCR Engine Error")
        mockRepository.mockResult = Result.failure(exception)

        // When
        val result = useCase(validBytes, 0)

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}

/**
 * OcrRepositoryのテスト用モック実装
 */
class MockOcrRepository : OcrRepository {
    var mockResult: Result<String> = Result.success("")
    var callCount = 0

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
        callCount++
        return mockResult
    }
}
