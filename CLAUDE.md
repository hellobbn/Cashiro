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
| Version name | `2.1.63` |
| Version code | `97` |
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
- A separate Home → Investments entry supports explicit read-only IBKR Flex connections; see `docs/brokerage-connections.md`. The provider interface is extensible; holdings do not modify bookkeeping balances or home net worth.
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

Semantic versions from upstream, currently `2.1.63` (`versionCode` 97).

- **MAJOR**: breaking changes
- **MINOR**: features
- **PATCH**: fixes

Bump `versionName` / `versionCode` in `app/build.gradle.kts` together.

## Commits and pull requests

- Do not add a `Claude-Session:` line (or any session link) to commit messages or PR
  descriptions.

## Module Structure

```
app/            Android application (namespace com.ritesh.cashiro)
parser-core/    JVM bank-SMS parsers (package com.ritesh.parser.core)
benchmark/      Macrobenchmark tests against app's `benchmark` build type
```

## Performance

- `app` has a `benchmark` build type: release code (R8, `-dontobfuscate`), debug-signed,
  `<profileable>`, applicationId `com.ritesh.cashiro.benchmark`.
- `app/src/benchmark` adds `SeedActivity`, which writes a fixed data set (4 CNY accounts,
  ~3,400 transactions) and skips onboarding. It exists only in benchmark builds.
- `.github/workflows/perf-device.yml` runs `:benchmark` on a physical phone in Firebase
  Test Lab (default Pixel 10 Pro, `model=blazer,version=36`). Needs the
  `FIREBASE_SERVICE_ACCOUNT` secret; runs on dispatch or `[ftl]` in a commit message on a
  non-`main` branch. Physical-device time is billed, so it does not run on every push.
- It A/B tests on one phone: a baseline (the base ref's `app/src/main`, replaced wholesale,
  built with this branch's benchmark setup and `-PbenchmarkIdSuffix=.benchmark.base`, so both
  builds install side by side) and the candidate, measured base, candidate, candidate, base
  in one run. The `variants` instrumentation argument sets that order; iteration counts in
  `ScreenBenchmark` are per round (latency tests 5, frame tests 2). Test names end in
  `[<index>]`, which `scripts/benchmark/compare.py --order` maps back.
- The two builds run as parallel jobs; the baseline APK is cached by base commit plus this
  branch's other build inputs. By default the run covers all of `ScreenBenchmark`.
- `compare.py` pools both rounds per build and reports, per scenario, both medians (frame
  metrics: percentiles), the change, a 95% bootstrap interval and a verdict (`RULES`):
  latency fails when the whole interval is past +10% and 10 ms (warns past +3% / 5 ms),
  frame overrun P90/P99 fails past +8 ms (warns past +2 ms), and more list publishes than
  base fail. A build whose two rounds disagree marks the metric unstable and downgrades a
  failure to a warning. The table goes to the job summary and to one comment on the
  branch's pull request, updated per run; the job fails on a regression.
- Commit message options: `[ftl base=<ref>]` picks the baseline (default `main`),
  `[ftl tests=a+b]` runs only those `ScreenBenchmark` methods, `[ftl compose-trace]` adds
  composable names to the traces (benchmark builds carry `runtime-tracing`; it slows
  composition, so use it to diagnose, not to compare). `[apks]` alone only builds: the
  `apk-base` and `apk-candidate` artifacts hold benchmark APKs that install side by side.
- `flingDownAndUp` swipes with UiObject2's fling gesture but not `fling()` itself: that waits
  5 s for a scroll-finished event Compose lists never send. It sleeps while the list coasts.
- Baseline Profile: `app/src/main/baseline-prof.txt`, installed on sideloaded builds by
  `profileinstaller`. `[ftl profile]` regenerates it: `BaselineProfileGenerator` runs on the
  unminified `profiling` build type (the benchmark build without R8, same package) on Test
  Lab and uploads the `baseline-profile` artifact; copy its `baseline-prof.txt` over the
  file (already filtered: no benchmark-only classes, and Kotlin `internal` names use the
  `app_standardRelease` module suffix). Regenerate after large UI changes. The `*WithProfile` tests compile each build with
  its own profile (`CompilationMode.Partial`), so base vs candidate shows what it wins.
- `BenchmarkBackupGenerator` (unit test, skipped unless `CASHIRO_BENCHMARK_BACKUP` is set)
  writes the same data set as an importable backup zip for testing on a real device.
- Never compare numbers across separate runs or devices; only within one run.
- Emulators were dropped: they render on the CPU, so frame times mostly measured
  SwiftShader, and the emulator process died flinging the Transactions list.
- `transactionsDataReady` times tab tap to the list on screen, which frame metrics miss
  because data loads off the main thread. It records no frame metrics: the window holds
  only 4-6 frames, so their percentiles are noise. `tapToListDrawnMs` reads it from the trace: tap
  (`deliverInputEvent`) to the `TransactionsList.firstDraw` section the app emits on the
  list's first draw; exact, and adds no work to the app. `transactionsDataReadyMs` polls
  with UiAutomator (wait-for-idle off, else it times "UI quiet for 500 ms") and includes
  its lookup overhead; it stays for baselines that predate the trace section.
- `./gradlew :app:compileStandardReleaseKotlin -PcomposeReports` writes Compose stability
  reports to `app/build/compose_compiler`.

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
