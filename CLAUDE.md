# Cashiro Project Context

## Project Overview

Cashiro is an Android expense tracker. This repository is a personal fork of
[ritesh-kanwar/Cashiro](https://github.com/ritesh-kanwar/Cashiro), itself based on
PennyWise AI.

The fork is for personal, mostly **manual** bookkeeping: accounts, categories,
budgets, and a Chinese / cross-border institution catalog. SMS parsing remains
in the tree from upstream but is not the product direction here. Do not add
Chinese bank SMS parsers unless explicitly requested.

## Identifiers

Do not rename these without an explicit migration plan. Changing `applicationId`
breaks updates of already-installed builds.

| What | Value |
|---|---|
| Gradle project | `cashiro-beta` |
| App namespace / applicationId | `com.ritesh.cashiro` |
| App source root | `app/src/main/java/com/ritesh/cashiro/` |
| Parser module | `parser-core` |
| Parser package | `com.ritesh.parser.core` |
| Parser source root | `parser-core/src/main/kotlin/com/ritesh/parser/core/` |
| Historical Room schema path | `app/schemas/com.pennywiseai.tracker.data.database.PennyWiseDatabase/` |
| Version name | `2.1.61-beta` |
| Version code | `94` |
| Min SDK | 26 |
| Compile / target SDK | 36 |
| License | AGPL-3.0 |

Room schemas keep the old `com.pennywiseai.tracker` directory name from the
PennyWise lineage. Leave that path alone.

## Important Documents

- **Architecture**: `/docs/architecture.md`
- **Design System**: `/docs/design.md`
- **Chinese experience**: `/docs/chinese-experience.md`
- **Validation**: `/docs/validation/chinese-experience/README.md`

## Key Technical Decisions

1. **UI**: Jetpack Compose + Material 3
2. **Architecture**: MVVM with UI / Domain / Data layers
3. **State**: Unidirectional Data Flow with StateFlow
4. **DI**: Hilt
5. **Database**: Room
6. **On-device model**: LiteRT-LM / MediaPipe (optional assistant)
7. **Background**: WorkManager (upstream SMS scan; not the focus of this fork)
8. **Flavors**: `standard` (default) and `fdroid`

## Current Direction

Personal Chinese / cross-border manual accounts:

- New installs default to CNY. Existing saved currencies are not overwritten.
- Institution picker covers CN / HK / SG / US banks and brokers as **name and icon presets only**.
- Choosing an institution does not add SMS parsing, login, or holdings sync.
- Prefer account UX, currency defaults, and imports over SMS automation.

## Design Principles

- Material You dynamic color on Android 12+
- Light / dark / dynamic themes
- 8dp grid
- Material 3 type scale
- NavigationBar on phones, NavigationRail on tablets
- Edge-to-edge via the existing scaffold / TopAppBar pattern
- Chinese UI should avoid awkward letter-spacing and should use `9月1日` style dates

## Code Style Guidelines

- Follow Kotlin conventions
- Use meaningful names
- Handle errors with sealed classes where the project already does
- Keep composables reusable
- Test light and dark themes
- Never put PII in comments or source

## Commands

```bash
./gradlew :app:assembleStandardDebug
./gradlew :app:testStandardDebugUnitTest
./gradlew :parser-core:test
./gradlew :app:lintStandardDebug
```

Debug APKs:

- `app/build/outputs/apk/standard/debug/app-standard-arm64-v8a-debug.apk`
- `app/build/outputs/apk/standard/debug/app-standard-universal-debug.apk`

CI publishes those to the rolling `debug-latest` GitHub Release on every `main` push.

## Versioning

Semantic versions from upstream, currently `2.1.61-beta` (`versionCode` 94).

- **MAJOR**: breaking changes
- **MINOR**: features
- **PATCH**: fixes

Bump `versionName` / `versionCode` in `app/build.gradle.kts` together.

## Module Structure

```
app/            Android application (namespace com.ritesh.cashiro)
parser-core/    JVM bank-SMS parsers (package com.ritesh.parser.core)
```

App packages:

```
com.ritesh.cashiro
├── data          Room, repositories, preferences, managers
├── domain        Use cases and models
├── presentation  Compose UI, feature ViewModels, navigation
├── di            Hilt modules
└── utils
```

## Bank Parser Architecture

Parsers live in `parser-core` so they stay free of Android dependencies.
This fork does not prioritize new parsers.

### If you must add a parser

1. Add it under `parser-core/src/main/kotlin/com/ritesh/parser/core/bank/`
2. Extend `BankParser`
3. Implement `getBankName()`, `canHandle(sender)`, `parse(smsBody, sender, timestamp)`
4. Override `extractAmount()` / `extractMerchant()` / `extractTransactionType()` as needed
5. Register it in `BankParserFactory.parsers`
6. Return `com.ritesh.parser.core.ParsedTransaction`
7. Map into the app with `com.ritesh.cashiro.data.mapper.toEntity()`

Parser tests must use the shared helpers in `ParserTestUtils`.
See `docs/parser-test-standards.md`.

## Lint

`app/lint.xml` plus the `lint {}` block in `app/build.gradle.kts`:

- Default English and `values-zh` are the maintained locales
- `values-zh-rTW` has Traditional Chinese for the new institution/settings strings
- `MissingTranslation` is ignored so stale Crowdin locales do not fail the build
- Dependency-version lint is disabled so builds are not blocked by upstream catalog drift
- `checkReleaseBuilds` is off; debug lint is `./gradlew :app:lintStandardDebug`
