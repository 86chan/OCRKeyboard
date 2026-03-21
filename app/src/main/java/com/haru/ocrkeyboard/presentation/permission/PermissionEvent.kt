package com.haru.ocrkeyboard.presentation.permission

/**
 * 権限に関するユーザーアクションおよびシステムイベント
 */
sealed interface PermissionEvent {
    /**
     * OSの最新権限状態に基づく同期
     *
     * @property isGranted 現在の許可状態
     */
    data class OnSyncStatus(val isGranted: Boolean) : PermissionEvent
    
    /** 権限の許可 */
    data object OnGranted : PermissionEvent
    
    /**
     * 権限の拒否
     *
     * @property shouldShowRationale 説明要否（falseの場合、今後のシステムダイアログの表示がブロックされたことを意味する）
     */
    data class OnDenied(val shouldShowRationale: Boolean) : PermissionEvent
    
    /** 設定誘導ダイアログの破棄 */
    data object OnDialogDismissed : PermissionEvent
}
