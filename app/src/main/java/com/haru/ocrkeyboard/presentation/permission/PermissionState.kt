package com.haru.ocrkeyboard.presentation.permission

/**
 * 権限状態の表現
 */
sealed interface PermissionState {
    /** 待機状態 (初期状態) */
    data object Idle : PermissionState
    /** 付与済みの状態 */
    data object Granted : PermissionState
    /** 一時拒否の状態（再リクエスト可能） */
    data object Denied : PermissionState
    /** 永久拒否の状態（設定画面操作必須） */
    data object PermanentlyDenied : PermissionState
}
