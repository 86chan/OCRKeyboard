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

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsRepository = SettingsRepository(context)
    }

    @Test
    fun defaultValuesAreFalse() = runTest {
        val useSwipe = settingsRepository.useSwipeGestureFlow.first()
        val useJapanese = settingsRepository.useJapaneseRecognitionFlow.first()

        assertEquals(false, useSwipe)
        assertEquals(false, useJapanese)
    }

    @Test
    fun setUseSwipeGesture_updatesValue() = runTest {
        settingsRepository.setUseSwipeGesture(true)
        val useSwipe = settingsRepository.useSwipeGestureFlow.first()
        assertEquals(true, useSwipe)

        settingsRepository.setUseSwipeGesture(false)
        val useSwipeFalse = settingsRepository.useSwipeGestureFlow.first()
        assertEquals(false, useSwipeFalse)
    }

    @Test
    fun setUseJapaneseRecognition_updatesValue() = runTest {
        settingsRepository.setUseJapaneseRecognition(true)
        val useJapanese = settingsRepository.useJapaneseRecognitionFlow.first()
        assertEquals(true, useJapanese)

        settingsRepository.setUseJapaneseRecognition(false)
        val useJapaneseFalse = settingsRepository.useJapaneseRecognitionFlow.first()
        assertEquals(false, useJapaneseFalse)
    }
}
