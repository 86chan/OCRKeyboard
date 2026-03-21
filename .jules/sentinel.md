
## 2024-03-21 - [SECURITY] Unnecessary Permissions Removed
**Vulnerability:** The Android application requested the `android.permission.INTERNET` permission in its AndroidManifest.xml, despite there being no network calls or internet functionality in the codebase.
**Learning:** Applications should adhere to the Principle of Least Privilege. Requesting unused permissions increases the attack surface and may unnecessarily concern privacy-conscious users.
**Prevention:** Regularly review AndroidManifest permissions against actual application functionality and remove any unused ones.
