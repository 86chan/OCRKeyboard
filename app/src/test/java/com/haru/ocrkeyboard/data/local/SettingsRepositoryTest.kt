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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepository
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    @Before
    fun setUp() {
        val testFile = tmpFolder.newFile("test_settings.preferences_pb")

        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )

        repository = SettingsRepository(testDataStore)
    }

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
