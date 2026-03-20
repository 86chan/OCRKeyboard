package com.haru.ocrkeyboard.service

import android.view.View
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
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
        viewModel = OcrKeyboardViewModel(useCase)

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
                val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
                val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
                currentInputConnection?.sendKeyEvent(downEvent)
                currentInputConnection?.sendKeyEvent(upEvent)
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
            
            win.decorView?.let { decorView ->
                setupViewTreeOwners(decorView)
            }
        }
        setupViewTreeOwners(composeView)
        
        return composeView
    }
}
