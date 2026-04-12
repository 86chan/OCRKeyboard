package com.haru.ocrkeyboard.domain.model

/**
 * OCR後処理における文字置換ルール
 *
 * @property from 認識された文字列（置換対象）
 * @property to 置換後の文字列
 * @property isEnabled ルールの有効状態
 */
data class CharReplacement(
    val from: String,
    val to: String,
    val isEnabled: Boolean = true,
)
