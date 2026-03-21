package com.haru.ocrkeyboard.presentation.permission

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 権限状態管理の責務を担うViewModel
 *
 * 単方向データフローによる状態遷移の制御
 */
class PermissionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PermissionState>(PermissionState.Idle)
    
    /** UIに反映される権限状態 */
    val uiState: StateFlow<PermissionState> = _uiState.asStateFlow()

    /**
     * イベントの評価と状態更新
     *
     * @param event 受信したイベント
     */
    fun handleEvent(event: PermissionEvent) {
        when (event) {
            is PermissionEvent.OnSyncStatus -> {
                _uiState.update { currentState ->
                    if (event.isGranted) {
                        PermissionState.Granted
                    } else {
                        // 永久拒否の案内を消さないための制御
                        currentState.takeIf { it != PermissionState.Granted } ?: PermissionState.Idle
                    }
                }
            }
            is PermissionEvent.OnGranted -> {
                _uiState.update { PermissionState.Granted }
            }
            is PermissionEvent.OnDenied -> {
                _uiState.update { 
                    if (event.shouldShowRationale) {
                        PermissionState.Denied
                    } else {
                        PermissionState.PermanentlyDenied
                    }
                }
            }
            is PermissionEvent.OnDialogDismissed -> {
                _uiState.update { PermissionState.Idle }
            }
        }
    }
}
