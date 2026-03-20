package com.haru.ocrkeyboard.service

import android.view.View
import android.view.KeyEvent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.haru.ocrkeyboard.data.local.SettingsRepository
import com.haru.ocrkeyboard.data.repository.OcrRepositoryImpl
import com.haru.ocrkeyboard.domain.usecase.RecognizeTextUseCase
import com.haru.ocrkeyboard.presentation.keyboard.OcrKeyboardIntent
import com.haru.ocrkeyboard.presentation.keyboard.OcrKeyboardScreen
import com.haru.ocrkeyboard.presentation.keyboard.OcrKeyboardViewModel
import kotlinx.coroutines.launch

/**
 * OCRキーボードのメインサービス
 */
class OcrKeyboardService : LifecycleInputMethodService() {

    private lateinit var viewModel: OcrKeyboardViewModel

    override fun onCreate() {
        super.onCreate()
        
        // 依存関係の手動注入
        val repository = OcrRepositoryImpl()
        val useCase = RecognizeTextUseCase(repository)
        val settingsRepository = SettingsRepository(applicationContext)
        viewModel = OcrKeyboardViewModel(useCase, settingsRepository)

        // ViewModelからのテキスト送信イベントの購読と適用
        lifecycleScope.launch {
            viewModel.commitTextEvent.collect { text ->
                currentInputConnection?.commitText(text, 1)
                viewModel.onIntent(OcrKeyboardIntent.TextCommitted)
            }
        }

        // ViewModelからのキーイベントの購読と送出
        lifecycleScope.launch {
            viewModel.keyEvent.collect { keyCode ->
                val ic = currentInputConnection ?: return@collect
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    // Backspaceは KeyEvent を直接送るのが最も互換性が高い（選択範囲の削除などもOS/アプリ側で処理される）
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
                } else {
                    val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
                    val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
                    ic.sendKeyEvent(downEvent)
                    ic.sendKeyEvent(upEvent)
                }
            }
        }
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            setContent {
                val state by viewModel.state.collectAsState()
                
                OcrKeyboardScreen(
                    state = state,
                    onIntent = viewModel::onIntent
                )
            }
        }
        
        // AndroidのComposeViewが所属するRootView(DecorView)に対してOwnerをセットする
        window?.window?.let { win ->
            val layoutParams = win.attributes
            layoutParams.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            win.attributes = layoutParams

            setupViewTreeOwners(win.decorView)
        }
        setupViewTreeOwners(composeView)
        
        return composeView
    }
}
