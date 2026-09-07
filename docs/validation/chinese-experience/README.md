# Chinese experience validation

Validated on 2026-09-07 with the standard debug build and Android 15 / API 35 emulator. Emulator screenshots use synthetic account data. The debug APK was subsequently installed and updated on the connected Pixel 9 Pro Fold at the user’s request.

## Build and tests

- `./gradlew :app:assembleStandardDebug :app:testStandardDebugUnitTest --offline --no-configuration-cache` — passed after the account-flow and asset changes. Packaging was rerun after the final icon-contrast adjustment.
- App JVM tests: **158 passed**, zero failures/skips. Includes currency migration, currency formatting/selection, Chinese date ranges, and institution aliases/region separation.
- Parser JVM tests: **395 passed**, zero failures/skips (`:parser-core:test`).
- `git -c core.whitespace=cr-at-eol diff --check` — passed. Some upstream Kotlin files use CRLF.
- Chinese/base primary string resources both contain **1,230 entries**; new feature strings also have Chinese equivalents.

## Emulator checks

- Chinese app locale (`zh`, without a country suffix) loads Chinese onboarding and new picker labels.
- Fresh manual onboarding starts in CNY; choosing an institution preserves that currency.
- Mainland and Hong Kong banks are separate, and `ibkr` / `chase` abbreviation searches return the correct bilingual entries.
- Account creation saves the institution name/logo and explicit USD selection. Reopening the account list shows the USD account.
- A duplicate name/account suffix shows a Chinese inline warning and leaves the form open.
- The account-number field remains above the keyboard and Save footer.
- Currency shortcuts begin CNY, HKD, USD, SGD, TWD, MOP, including localized currency names.
- Home displays net worth while collapsed and identifies broker presets as investment accounts.
- Light/dark layouts and 1.3× text checked on a 720×1600 display at 320 dpi.

## Known validation limits

Full Android lint is **not clean**: the run produced 247 errors, including unrelated API/Compose checks and translation/plural/format issues across the existing language files. Eighteen MissingTranslation reports concern new English/Chinese labels in other locales, where Android uses the English fallback. The report is at `app/build/reports/lint-results-standardDebug.html` (generated build output). This report preceded the last account-sheet save/layout refinements; the final APK and app tests include those refinements. Lint failures have not been suppressed.

The software-rendered emulator twice crashed at the host level; UI checks then ran successfully using `-gpu host -feature -Vulkan`. Dependency TLS failures during initial setup were worked around by retrying downloads; subsequent debug builds used the cache offline. The first release build downloaded additional release dependencies.

## Deliverables

- ARM64 debug APK: `app/build/outputs/apk/standard/debug/app-standard-arm64-v8a-debug.apk`
- Universal debug APK: `app/build/outputs/apk/standard/debug/app-standard-universal-debug.apk`
- Feature notes: [Chinese experience](../../chinese-experience.md)
- Logo provenance: [Institution assets](../../institution-assets.md)

Debug APKs use the local debug signing key. An installed release signed with another key cannot be updated directly by this APK; use matching signing credentials to preserve an existing installation.

## Screenshots

- [Home, light](home-light.png) / [Home, dark](home-dark.png)
- [Settings, light](settings-light.png) / [Settings, dark with larger text](settings-dark-large-text.png)
- [Currency shortcuts](currencies.png)
- [Account input with keyboard](account-keyboard.png)
- [USD account](account-usd.png)
- [Duplicate account warning](duplicate-account.png)
- [Institution picker](institutions-cn.png)

## Android 17 RELRO warning fix

The physical Pixel 9 Pro Fold reported “RELRO alignment check failed” for `libdatastore_shared_counter.so` from DataStore 1.2.0. Updated DataStore to 1.2.1, whose rebuilt native library has a 16 KB-aligned RELRO end. The patch release has no DataStore API or behavior changes ([official release notes](https://developer.android.com/jetpack/androidx/releases/datastore#1.2.1)).

- Rebuilt standard debug APKs and reran all 158 app JVM tests successfully.
- ARM64 APK passed SDK `zipalign -c -P 16 -v 4` verification.
- Updated the installed app with `adb install -r`, preserving app data.
- Android package `pageSizeCompat` changed from `256` to `0`.
- Dismissed the old dialog using **OK**, then reopened the app; no compatibility warning appeared. Warning suppression was not enabled.
- The physical phone currently uses 4 KB pages (`getconf PAGE_SIZE` returned `4096`); this verifies Android 17 package validation and startup, not execution on a 16 KB kernel.

The before/after phone screenshots are kept locally under `/tmp/cashiro-phone-warning2.png` and `/tmp/cashiro-phone-fixed.png`; they are not committed because they include personal system overlays.

## Signed release installation

- `./gradlew :app:assembleStandardRelease --no-configuration-cache` passed, including release optimization and `lintVitalStandardRelease`.
- ARM64 release APK passed `apksigner verify` and `zipalign -c -P 16 4`.
- Installed and cold-launched on the connected XT2651-4 running Android 16 after the user removed the previously installed app.
- Package flags exclude `DEBUGGABLE`; `pageSizeCompat=0`.
- Release APK: `app/build/outputs/apk/standard/release/app-standard-arm64-v8a-release.apk`.

The release uses a persistent local fork signing key, not the upstream key. The keystore and signing credentials are ignored by Git and must be backed up securely for future compatible updates. APKs and private device screenshots are also excluded from this commit.
