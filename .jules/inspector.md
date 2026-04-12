
## 2025-05-19 12:30:00 - [Test Empty Image Recognition Result]
**Learning:** In `OcrKeyboardViewModel`, when the text recognition use case returns an empty string (or whitespace-only string after applying delimiters), the system branches into an error state logic, updating the state to display "テキストが検出されませんでした" (No text detected). This error path had no explicit unit test coverage, which could lead to accidental regressions where empty results silently fail or cause unexpected UI states.
**Action:** Always add tests to verify ViewModel state transitions when a domain usecase returns successful but empty or null-equivalent data, ensuring the "not found" UI error state is properly handled.
