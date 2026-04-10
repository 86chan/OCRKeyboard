# アーキテクチャ概要

本書は、OCR Keyboardアプリケーションのコアアーキテクチャとデータフローについて説明します。

## コアコンポーネント

1.  **`OcrKeyboardService`**: アプリケーションのエントリーポイント。`LifecycleInputMethodService`（Jetpack ComposeをサポートするためのAndroidの`InputMethodService`のラッパー）を拡張。`InputConnection`を介したUI層とAndroidの入力システムの橋渡し。
2.  **UI層 (Jetpack Compose)**: `OcrKeyboardScreen`とそのサブコンポーネントによるカメラプレビュー、コントロール、認識結果の描画。UIの単方向データフロー（UDF）の厳格な遵守。
3.  **`OcrKeyboardViewModel`**: UIの状態（`OcrKeyboardState`）の管理、およびユーザーのインテント（`OcrKeyboardIntent`）の処理。UI層とドメイン層間のメディエーター機能。
4.  **ドメイン・データ層**: `RecognizeTextUseCase`によるOCRロジックのカプセル化と`OcrRepositoryImpl`への委譲。リポジトリによる`BitmapRegionDecoder`を使用した画像の切り抜きや、Google ML Kitを使用したテキスト抽出などの処理。

## OCRデータフロー

以下のシーケンス図は、ユーザーが画像をキャプチャしてから、対象のアプリケーションにテキストが挿入されるまでのエンドツーエンドのプロセスを示しています。

```mermaid
sequenceDiagram
    autonumber
    actor User as ユーザー
    participant UI as OcrKeyboardScreen (Compose)
    participant VM as OcrKeyboardViewModel
    participant UC as RecognizeTextUseCase
    participant Repo as OcrRepositoryImpl (ML Kit)
    participant Service as OcrKeyboardService (IME)
    participant App as Target App (InputConnection)

    User->>UI: 「スキャン」ボタンをタップ
    UI->>VM: onIntent(OcrKeyboardIntent.RecognizeText)
    VM->>VM: 状態更新 (isRecognizing = true)

    VM->>UC: invoke(imageBytes, rotation...)
    UC->>Repo: extractText(...)

    rect rgb(200, 200, 200)
        Note over Repo: Native ML Kit Processing
        Repo->>Repo: 画像領域のデコード (BitmapRegionDecoder)
        Repo->>Repo: TextRecognitionクライアントによる処理
        Repo->>Repo: テキストブロックのソートとフォーマット
    end

    Repo-->>UC: Result.success(text)
    UC-->>VM: Result.success(text)

    VM->>VM: 候補の生成と状態更新
    VM-->>UI: 状態更新 (候補 / 結果の表示)

    alt 自動入力 (候補なし)
        VM->>Service: emit(commitTextEvent)
    else ユーザーが候補を選択
        User->>UI: 候補をタップ
        UI->>VM: onIntent(OcrKeyboardIntent.SuggestionSelected)
        VM->>Service: emit(commitTextEvent)
    end

    Service->>App: currentInputConnection.commitText(text)
    Service->>VM: onIntent(OcrKeyboardIntent.TextCommitted)
    VM->>VM: 状態更新 (テキストのクリア)
```

## 主要なアーキテクチャ制約

*   **ローカル処理のみ**: すべてのML Kit処理のデバイス上での実行。画像や抽出されたテキストの外部サーバーへの送信の禁止。
*   **単方向データフロー (UDF)**: UIコンポーネントによる直接的な状態変更やリポジトリとの通信の禁止。すべてのアクションのインテントを介したViewModelへの伝達。
*   **メモリ効率**: `Bitmap.Config.RGB_565`を指定した`BitmapRegionDecoder`の使用による、ML Kitへの引き渡し前の高解像度カメラ映像切り抜き時のメモリ割り当ての最小化。
