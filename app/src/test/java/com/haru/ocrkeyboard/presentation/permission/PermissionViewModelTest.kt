package com.haru.ocrkeyboard.presentation.permission

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * PermissionViewModelの振る舞いを検証するテストクラス。
 */
class PermissionViewModelTest {

    private lateinit var viewModel: PermissionViewModel

    /**
     * テストごとの初期化処理。
     */
    @Before
    fun setUp() {
        viewModel = PermissionViewModel()
    }

    /**
     * OSと同期し権限が付与されていた場合、状態がGrantedに遷移することの検証。
     *
     * [事前条件 (Given)]
     * ViewModelが初期状態（Idle）であること。
     *
     * [実行 (When)]
     * isGrantedがtrueの状態でOnSyncStatusイベントを送信する。
     *
     * [検証 (Then)]
     * uiStateがPermissionState.Grantedに更新されること。
     */
    @Test
    fun handleEvent_onSyncStatusGranted_updatesStateToGranted() {
        // When
        viewModel.handleEvent(PermissionEvent.OnSyncStatus(isGranted = true))

        // Then
        assertEquals(PermissionState.Granted, viewModel.uiState.value)
    }

    /**
     * OSと同期して未付与であった場合、初期状態（Idle）が維持されることの検証。
     *
     * [事前条件 (Given)]
     * ViewModelが初期状態（Idle）であること。
     *
     * [実行 (When)]
     * isGrantedがfalseの状態でOnSyncStatusイベントを送信する。
     *
     * [検証 (Then)]
     * uiStateがPermissionState.Idleのままであること。
     */
    @Test
    fun handleEvent_onSyncStatusDenied_maintainsIdleState() {
        // When
        viewModel.handleEvent(PermissionEvent.OnSyncStatus(isGranted = false))

        // Then
        assertEquals(PermissionState.Idle, viewModel.uiState.value)
    }

    /**
     * 権限が許可された場合、状態がGrantedに遷移することの検証。
     *
     * [事前条件 (Given)]
     * ViewModelが初期状態（Idle）であること。
     *
     * [実行 (When)]
     * OnGrantedイベントを送信する。
     *
     * [検証 (Then)]
     * uiStateがPermissionState.Grantedに更新されること。
     */
    @Test
    fun handleEvent_onGranted_updatesStateToGranted() {
        // When
        viewModel.handleEvent(PermissionEvent.OnGranted)

        // Then
        assertEquals(PermissionState.Granted, viewModel.uiState.value)
    }

    /**
     * 権限が拒否され、かつ説明が必要な場合、状態がDeniedに遷移することの検証。
     *
     * [事前条件 (Given)]
     * ViewModelが初期状態（Idle）であること。
     *
     * [実行 (When)]
     * shouldShowRationaleがtrueの状態でOnDeniedイベントを送信する。
     *
     * [検証 (Then)]
     * uiStateがPermissionState.Deniedに更新されること。
     */
    @Test
    fun handleEvent_onDeniedWithRationale_updatesStateToDenied() {
        // When
        viewModel.handleEvent(PermissionEvent.OnDenied(shouldShowRationale = true))

        // Then
        assertEquals(PermissionState.Denied, viewModel.uiState.value)
    }

    /**
     * 権限が拒否され、かつ説明が不要（今後表示しない）の場合、状態がPermanentlyDeniedに遷移することの検証。
     *
     * [事前条件 (Given)]
     * ViewModelが初期状態（Idle）であること。
     *
     * [実行 (When)]
     * shouldShowRationaleがfalseの状態でOnDeniedイベントを送信する。
     *
     * [検証 (Then)]
     * uiStateがPermissionState.PermanentlyDeniedに更新されること。
     */
    @Test
    fun handleEvent_onDeniedWithoutRationale_updatesStateToPermanentlyDenied() {
        // When
        viewModel.handleEvent(PermissionEvent.OnDenied(shouldShowRationale = false))

        // Then
        assertEquals(PermissionState.PermanentlyDenied, viewModel.uiState.value)
    }
}
