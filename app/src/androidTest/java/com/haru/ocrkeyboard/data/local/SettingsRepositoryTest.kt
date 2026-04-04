package com.haru.ocrkeyboard.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsRepository = SettingsRepository(context)
    }

    @After
    fun tearDown() {
        File(context.filesDir, "datastore/settings.preferences_pb").delete()
    }

    @Test
    fun defaultValuesAreFalse() = runBlocking {
        val useSwipe = settingsRepository.useSwipeGestureFlow.first()
        val useJapanese = settingsRepository.useJapaneseRecognitionFlow.first()

        assertEquals(false, useSwipe)
        assertEquals(false, useJapanese)
    }

    @Test
    fun setUseSwipeGesture_updatesValue() = runBlocking {
        settingsRepository.setUseSwipeGesture(true)
        val useSwipe = settingsRepository.useSwipeGestureFlow.first()
        assertEquals(true, useSwipe)

        settingsRepository.setUseSwipeGesture(false)
        val useSwipeFalse = settingsRepository.useSwipeGestureFlow.first()
        assertEquals(false, useSwipeFalse)
    }

    @Test
    fun setUseJapaneseRecognition_updatesValue() = runBlocking {
        settingsRepository.setUseJapaneseRecognition(true)
        val useJapanese = settingsRepository.useJapaneseRecognitionFlow.first()
        assertEquals(true, useJapanese)

        settingsRepository.setUseJapaneseRecognition(false)
        val useJapaneseFalse = settingsRepository.useJapaneseRecognitionFlow.first()
        assertEquals(false, useJapaneseFalse)
    }
}
