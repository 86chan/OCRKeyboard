# Architecture Overview

This document outlines the core architecture and data flow for the OCR Keyboard application.

## Core Components

1.  **`OcrKeyboardService`**: The entry point of the application. It extends `LifecycleInputMethodService` (a wrapper around Android's `InputMethodService` to support Jetpack Compose). It is responsible for bridging the UI layer with the Android input system via `InputConnection`.
2.  **UI Layer (Jetpack Compose)**: `OcrKeyboardScreen` and its sub-components render the camera preview, controls, and recognition results. The UI strictly adheres to Unidirectional Data Flow (UDF).
3.  **`OcrKeyboardViewModel`**: Manages the UI state (`OcrKeyboardState`) and processes user intents (`OcrKeyboardIntent`). It acts as a mediator between the UI and the Domain layer.
4.  **Domain & Data Layers**: `RecognizeTextUseCase` encapsulates the OCR logic, delegating to `OcrRepositoryImpl`. The repository handles the heavy lifting of image cropping via `BitmapRegionDecoder` and text extraction using Google ML Kit.

## OCR Data Flow

The following sequence diagram illustrates the end-to-end process from the moment a user captures an image to the text being inserted into the target application.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant UI as OcrKeyboardScreen (Compose)
    participant VM as OcrKeyboardViewModel
    participant UC as RecognizeTextUseCase
    participant Repo as OcrRepositoryImpl (ML Kit)
    participant Service as OcrKeyboardService (IME)
    participant App as Target App (InputConnection)

    User->>UI: Taps "Capture" button
    UI->>VM: onIntent(OcrKeyboardIntent.RecognizeText)
    VM->>VM: Update State (isRecognizing = true)

    VM->>UC: invoke(imageBytes, rotation...)
    UC->>Repo: extractText(...)

    rect rgb(200, 200, 200)
        Note over Repo: Native ML Kit Processing
        Repo->>Repo: Decode image region (BitmapRegionDecoder)
        Repo->>Repo: Process with TextRecognition client
        Repo->>Repo: Sort and format text blocks
    end

    Repo-->>UC: Result.success(text)
    UC-->>VM: Result.success(text)

    VM->>VM: Generate suggestions & Update State
    VM-->>UI: State update (Show suggestions / result)

    alt Auto-commit (No suggestions)
        VM->>Service: emit(commitTextEvent)
    else User selects suggestion
        User->>UI: Taps suggestion
        UI->>VM: onIntent(OcrKeyboardIntent.SuggestionSelected)
        VM->>Service: emit(commitTextEvent)
    end

    Service->>App: currentInputConnection.commitText(text)
    Service->>VM: onIntent(OcrKeyboardIntent.TextCommitted)
    VM->>VM: Update State (Clear text)
```

## Key Architectural Constraints

*   **Local Processing Only**: All ML Kit processing occurs on-device. No images or extracted text are ever sent to a remote server.
*   **Unidirectional Data Flow (UDF)**: The UI components do not directly modify state or communicate with the repository. All actions flow through intents to the ViewModel.
*   **Memory Efficiency**: The repository uses `BitmapRegionDecoder` with `Bitmap.Config.RGB_565` to minimize memory allocation when cropping high-resolution camera feeds before passing them to ML Kit.
