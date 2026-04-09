## 2025-03-20 - [Avoid Rotating Large Bitmaps Before Cropping]
**Learning:** Full-resolution camera images take a huge amount of memory to rotate in Android `Bitmap.createBitmap`. In `OcrRepositoryImpl`, rotating a large bitmap just to crop it creates a massive memory overhead and slows down processing. Instead of rotating the bitmap before cropping, you can calculate the crop boundaries on the rotated coordinate system and map them back to the unrotated coordinate system. MLKit's `InputImage` can handle the rotation itself, meaning we only ever create a much smaller, cropped bitmap, saving significant memory and CPU time.
**Action:** When working with image cropping APIs that receive rotated frames (like CameraX + MLKit), always map the crop area to the original frame's coordinate space and pass the rotation angle to the processing API, rather than manually allocating memory for a fully rotated image.

## 2025-03-22 - [Defer State Reads in Compose Canvas]
**Learning:** In Compose, reading primitive `Float` values (from mutableState) inside a `@Composable` block triggers full recomposition of that component whenever the value changes. When values change extremely rapidly (like during a `pointerInput` pinch/swipe gesture), this causes severe performance drops. However, passing these values as lambda providers (`() -> Float`) and invoking them *inside* the `Canvas` `DrawScope` skips the Composition phase entirely and only triggers the Draw phase.
**Action:** Always use deferred state reading (lambda providers) for values that update on every frame during animations or gestures, especially for `Canvas` inputs.

## 2025-03-23 - [Avoid Hoisting Rapid-Changing State to Top-Level Screens]
**Learning:** In Compose, hoisting state variables (like `isDeletePressed`) that change frequently during user interaction (e.g., gestures, animations) to the top-level parent component (like `OcrKeyboardContent`) causes the entire screen to recompose on every change. This forces Compose to re-evaluate the entire layout tree, including expensive elements like `AndroidView` (CameraX preview) and custom `Canvas` overlays, resulting in noticeable UI stuttering.
**Action:** Encapsulate rapid-changing interaction states and their associated logic (like `LaunchedEffect` for auto-repeat) inside the smallest possible leaf components (e.g., a custom `DeleteButton`). By exposing only standard event callbacks (like `onDelete`) to the parent, you constrain recompositions entirely to the component that visually changes.

## 2025-03-24 - [Halve Bitmap Memory in OCR pipeline using RGB_565]
**Learning:** By default, Android's `BitmapFactory` decodes images into the `ARGB_8888` configuration, which consumes 4 bytes per pixel. However, ML Kit's Vision and Text Recognition APIs do not require an alpha channel (they rely on luminance/RGB). Decoding camera byte arrays into `Bitmap.Config.RGB_565` consumes exactly 50% less memory (2 bytes per pixel) without negatively impacting text recognition accuracy.
**Action:** Always configure `BitmapFactory.Options.inPreferredConfig = Bitmap.Config.RGB_565` when decoding images that will be fed exclusively to ML Kit pipelines or any other computer vision API that does not require transparency.

## 2025-04-03 - [Offload Heavy CPU Operations to Background Thread in Coroutines]
**Learning:** In Android, invoking a `suspendCancellableCoroutine` block directly from the Main thread executing heavy CPU-bound tasks (like `BitmapFactory.decodeByteArray` or `BitmapRegionDecoder.decodeRegion`) will block the UI thread, causing jank and unresponsiveness. Using a `suspend` keyword does not inherently execute the code on a background thread; it merely suspends execution of the coroutine.
**Action:** When creating a `suspend` function that performs heavy CPU operations within a coroutine, wrap the block with `withContext(Dispatchers.Default)` to ensure the workload is offloaded to a background thread pool, maintaining a smooth and responsive UI.

## 2025-04-04 - [Optimize CameraX Capture Latency]
**Learning:** In CameraX, the default `ImageCapture.Builder().build()` uses `CAPTURE_MODE_MAXIMIZE_QUALITY`. This mode prioritizes image quality (HDR, noise reduction), which causes a noticeable shutter delay (~500ms). For OCR applications where pure speed is preferred over post-processing, this latency is detrimental.
**Action:** Always configure `ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)` when capturing images for computer vision pipelines where speed is critical.

## 2025-04-04 - [Lazy Initialize ML Kit Models]
**Learning:** Eagerly initializing ML Kit clients (`TextRecognition.getClient(...)`) as standard properties in a Repository or Service loads the underlying models into memory immediately upon instantiation. In a custom keyboard service, the user might open the keyboard just to type, never triggering the OCR scanner. Eager initialization in this scenario needlessly inflates startup time and memory usage.
**Action:** Always use `by lazy` for instantiating ML Kit clients or heavy SDK objects so they are only loaded into memory when their specific functionality is actually requested by the user.

## 2025-04-04 - [Avoid List Allocation in Compose Gesture Loops]
**Learning:** In Jetpack Compose, processing gesture inputs inside a `do-while` loop with `awaitPointerEvent()` happens very frequently. Using standard collection operations like `event.changes.filter { ... }` or `event.changes.any { ... }` allocates a new list or iterator on every single frame. This triggers frequent Garbage Collection, leading to visible UI stuttering (jank).
**Action:** Always replace standard `.filter {}` and `.any {}` calls in high-frequency compose blocks with `androidx.compose.ui.util.fastForEach` and `fastAny`. When you need to filter and count, use local integer variables to track counts and target indices rather than allocating temporary lists.

## 2025-04-10 - [Avoid Nested Iteration on Sorted Data]
**Learning:** In `OcrRepositoryImpl`, when grouping OCR text lines into rows, the lines are pre-sorted by their vertical position (`top` coordinate). Searching the entire list of already-created rows using `.find` to see which row a line belongs to creates an O(N²) nested loop. However, because the data is strictly sorted, a new line will only ever mathematically belong to the *most recently created* row, or it will start a new row. There is no need to iterate backwards through older rows.
**Action:** When grouping linearly dependent data that has been pre-sorted, replace `O(N)` search operations like `.find` with `O(1)` operations like `.lastOrNull()` to eliminate nested loops and turn O(N²) complexity into O(N).

## 2025-05-19 - [Consolidating Loops Using fastForEach in Compose]
**Learning:** In Jetpack Compose, functions like `fastForEach` and `fastAny` from `androidx.compose.ui.util` are inline functions that compile down to standard index-based `for` loops without the overhead of Iterator creation. Therefore, manually writing indexed `for` loops does not provide additional performance benefits over these utilities and harms readability. However, running multiple consecutive passes over the same collection (like `event.changes.fastForEach { ... }` followed by `event.changes.fastAny { ... }`) does result in $O(2N)$ execution. We can optimize this to $O(N)$ without losing readability by combining the checks inside a single `fastForEach` loop using a simple tracking variable.
**Action:** When tracking gesture events, avoid calling `.fastAny { it.pressed }` immediately after `.fastForEach { ... }`. Instead, calculate the `pressed` condition inside the existing `fastForEach` block. Do not manually replace `fastForEach` with index-based `for` loops.
