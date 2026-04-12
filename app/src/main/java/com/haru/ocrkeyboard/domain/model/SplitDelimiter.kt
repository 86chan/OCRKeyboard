package com.haru.ocrkeyboard.domain.model

/**
 * 候補生成時の分割に使用する区切り文字モデル
 *
 * @property char 1文字の区切り文字
 * @property isEnabled 分割ルール自体の有効/無効
 * @property trimSurroundingSpaces 正規化時にこの文字の前後に存在する空白（半角・全角）を削除するか
 */
data class SplitDelimiter(
    val char: String,
    val isEnabled: Boolean = true,
    val trimSurroundingSpaces: Boolean = false,
)
