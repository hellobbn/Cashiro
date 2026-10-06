# Cashiro Project Context

## Project Overview

Cashiro is an Android expense tracker. This repository is a personal fork of
[ritesh-kanwar/Cashiro](https://github.com/ritesh-kanwar/Cashiro), itself based on
PennyWise AI.

The fork is for personal, mostly **manual** bookkeeping: accounts, categories,
budgets, and a Chinese / cross-border institution catalog. Upstream's SMS import
has been removed entirely: the `parser-core` module, its settings and, in migration 69→70, the
columns it filled (a subscription's notes, once kept in `sms_body`, are now `notes`). Do not
bring SMS import back unless explicitly requested. (Lend/borrow can still open the messaging
app to send someone a reminder; that is unrelated.)

## Identifiers

Do not rename these without an explicit migration plan. Changing `applicationId`
breaks updates of already-installed builds.

| What | Value |
|---|---|
| Gradle project | `cashiro-beta` |
| App namespace / applicationId | `com.ritesh.cashiro` |
| App source root | `app/src/main/java/com/ritesh/cashiro/` |
| Room schema path | `app/schemas/com.ritesh.cashiro.data.database.CashiroDatabase/` (database version 70) |
| Historical Room schema paths | `app/schemas/com.pennywiseai.tracker.data.database.PennyWiseDatabase/`, `app/schemas/com.ritesh.cashiro.data.database.PennyWiseDatabase/` |
| Version name | `2.1.63` |
| Version code | `97` |
| Min SDK | 26 |
| Compile SDK | 37 |
| Target SDK | 36 |
| License | AGPL-3.0 |

The historical schema directories keep the old PennyWise names. Leave those paths alone.
Migrations live in `data/database/Migrations.kt`; `CashiroDatabase.MIGRATIONS` lists the manual
ones, and `MigrationChainTest` opens every exported schema at the current version through them, so
a new version needs its schema exported and its migration added there. First-run seeding is in
`data/database/DatabaseCallback.kt`.

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
6. **AI**: cloud only, with the user's own key (see "AI bookkeeping")
7. **Background**: WorkManager for scheduled cloud backups; AlarmManager for the daily reminder
8. **Flavors**: `standard` (default) and `fdroid`; they share all code

## Current Direction

Personal Chinese / cross-border manual accounts:

- New installs default to CNY. Existing saved currencies are not overwritten.
- Institution picker covers CN / HK / SG / US banks and brokers as **name and icon presets only**.
- Choosing an institution does not add login or holdings sync.
- A separate Home → Investments entry supports explicit read-only IBKR Flex connections; see `docs/brokerage-connections.md`. The provider interface is extensible; holdings do not modify bookkeeping balances or home net worth. Backups carry connections and tokens (`brokerage.json`) only when the user ticks it on export or the cloud backup is end-to-end encrypted; device sync never does.
- Prefer account UX, currency defaults, and imports over automation.
- Removed, do not reintroduce without asking: Play in-app update/review, smart rules,
  webhooks, merchant mappings, Indian e-mandate subscriptions, sample data and the developer
  page, the "manually added only" budget mode, account hiding/merging, selective import and
  masked export. Migration 68→69 dropped their tables; old backups that carry them still import.
- Credit cards may have a statement day and a due day (`accounts.statement_day` / `due_day`);
  the account page shows the bill (`cardStatus`): the statement less repayments since, overdue
  until paid.
- A transaction's balance effect is defined once (`balanceMoves`), used by adding and editing.
- Merchant icons: a transaction shows the icon the user picked for its merchant name
  (`MerchantIconStore`, files in `filesDir/merchant_icons`, carried in backups), else a bundled
  brand logo (`BrandIcons`), else its subcategory or category icon. The picker opens from the icon on
  the transaction detail and searches Apple's App Store (`AppStoreIconSearch`, country `cn`) only
  when the user does; nothing is fetched automatically.
- Multi-currency accounts: an account is a row in `accounts` (main currency, kind, limit, look)
  and holds one or more currencies in `account_currencies` (added in the account sheet or when a
  transfer target lacks one; never removed or hidden). Balance rows carry `account_id` and the
  currency ("pocket") they belong to. A transaction lands in its currency's pocket when the account
  holds it, else in the main one (`pocketCurrency`). A transfer stores its target as
  `to_account_id` + `to_currency`, and what arrived in `to_amount` when the currencies differ
  (two currencies of one account included). The account card's big number is the total in the
  app's main currency (`AccountHoldingsSource`), with each currency listed under it.
- Credit cards can store a statement day and a due day (`accounts.statement_day` / `due_day`, days of
  the month; a short month uses its last day, `CardCycle`). The account page shows the open
  statement, what of it is still owed and when it is due; Home's credit card row shows the nearest
  due date.
- Balances: a credit card's balance is what is owed and may go negative (overpaid). Edits go
  through `TransactionEditor`: undo the old effect as a delete does, apply the new one as an add does.
- Most code still finds an account by bank name + last 4 (cards, templates, budgets, preferences).
  Rename through `AccountRenamer`, which moves the account row, balances, transactions, cards,
  templates, budgets, subscriptions and the hidden/main preferences in one transaction.
- Backups carry `accounts` and `account_currencies`; older ones are restored the way migration
  66→67 converts a database (stray currencies on an account's rows join its main currency).

## AI bookkeeping

Cloud only, with the user's own key; no on-device model. Plain HTTP over the existing Ktor
client, no provider SDKs, to keep the app small.

- `data/ai/AiChat.kt` speaks two protocols: the Claude Messages API and OpenAI-compatible Chat
  Completions (OpenRouter, OpenAI, DeepSeek, Qwen…). Replies are appended verbatim (thinking blocks
  included). Server-side refusal fallbacks are only sent to `api.anthropic.com`.
- `AiSettings` keeps protocol, address, model and key in encrypted preferences.
- Once a key is entered, the form lists the provider's models (`AiChat.listModels`) and the user
  picks one from a searchable sheet (or types an id the list lacks). OpenRouter models that cannot
  call tools are left out, and those that read images are marked.
- `LedgerTools` is the API a model gets:
  - `find_transactions` answers at once.
  - `add_transactions`, `update_transactions`, `delete_transactions` and `create_account` only queue
    a `LedgerChange`.
  - `create_account` returns a ref (`N1`…) the same session's transactions can use. Icon, color and
    currency come from `InstitutionCatalog` when the name matches.
  - Each account ref lists the currencies it holds. A transaction is in one of its account's
    currencies (`currency`, default the main one); a transfer picks the target's with `to_currency`
    and states what arrived in `to_amount` when they differ.
  - `set_balance` calibrates one currency of a listed account (and the card limit) with a new
    `BALANCE_CALIBRATION` row, as the app's own balance edit does.
  - `update_account` renames an account, changes its kind or credit limit, or adds currencies
    (`add_currencies`, applied before the transactions in them); a rename goes through
    `AccountRenamer`. Main currency and last 4 are not changeable, and the model never deletes
    accounts or currencies.
  - Updates touch merchant, category, subcategory and notes only. Amount, date, type or account
    changes are a delete plus an add.
  - `apply` goes through `AddTransactionUseCase` and the repository's delete, so balances stay
    right, and returns what `undo` needs. New accounts are created first; a transaction on a new
    account the user left out is saved without an account. Balance calibrations and account edits
    come last, so a rename also carries the transactions just added.
- `AiLedgerSession` runs the tool loop. The accounts (as refs `A1`, `A2`…) and the categories are
  in the system prompt. The model need not look up duplicates (drafts flag them) and ends with a
  `finish` call in the same reply as its proposals, so a plain import is one round trip; a reply
  with a rejected call gets another turn. Each request, reply (thinking, text, tool calls, tokens,
  time), lookup and proposal is an `AiStep`; `AiProgress.kt` shows them as a timeline whose rows
  open to the raw text. The run is kept with the review (and after saving) under a collapsed
  "How it was done" row with its rounds, time and tokens.
- `AiAttachmentReader` handles the input files:
  - Tall screenshots are cut into tiles.
  - PDFs go to Claude as documents and to other providers as their text layer, or as the file
    when they have none. Password-protected PDFs are unlocked locally.
  - Text files are decoded as UTF-8, falling back to GB18030.
- UI: `AiAssistantScreen`, opened from the ✨ button on Home or Settings → AI bookkeeping.
  - Files shared to Cashiro (`SEND` / `SEND_MULTIPLE` on `MainActivity`) are copied into
    `AiShareInbox` at once (the read permission ends with the activity). They stay there until
    used or removed. The screen opens only once the app-lock state is loaded and unlocked.
  - The review (`AiReview.kt`) shows proposed changes as transaction rows, grouped into new,
    edits and deletions. A row opens a sheet where a new transaction can be corrected, or any
    change left out. Likely duplicates start left out; nothing is written before Save.

## Design Principles

- Material You dynamic color on Android 12+
- Light / dark / dynamic themes
- 8dp grid
- Material 3 type scale
- Adaptive layout (`presentation/ui/adaptive/WindowLayout.kt`, read from the window width):
  - Below 600 dp: bottom NavigationBar.
  - From 600 dp: NavigationRail at the start edge on every screen but lock/onboarding.
    Screens other than the three main tabs are at most 720 dp wide, centered (`ReadableWidth`).
  - From 720 dp:
    - Home and Analytics are two columns.
    - Transactions and the account lists are list-detail (`TransactionDetailPane`,
      `AccountDetailPane`); system back closes an open pane first.
- Edge-to-edge via the existing scaffold / TopAppBar pattern
- Material 3 Expressive components through the shared wrappers (table in `docs/design.md` →
  "Material 3 Expressive conventions"): `CustomTitleTopAppBar` (flexible app bar), `TooltipIconButton`,
  `GenericTypeSwitcher` (connected button group), `CashiroSwitch` / `PreferenceSwitch`, M3E loading
  indicators, standard snackbars with Undo. Theme-aware colors come from `isAppInDarkTheme`, not
  `isSystemInDarkTheme()`.
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
./gradlew :app:lintStandardDebug
```

Debug APKs:

- `app/build/outputs/apk/standard/debug/app-standard-arm64-v8a-debug.apk`
- `app/build/outputs/apk/standard/debug/app-standard-universal-debug.apk`

CI publishes those to the rolling `debug-latest` GitHub Release on every `main` push. The Tests
workflow runs all app unit tests, debug lint and the F-Droid compile.

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
benchmark/      Macrobenchmark tests against app's `benchmark` build type
```

## Performance

- `app` has a `benchmark` build type: release code (R8, `-dontobfuscate`), debug-signed,
  `<profileable>`, applicationId `com.ritesh.cashiro.benchmark`.
- `app/src/benchmark` adds `SeedActivity`, which writes a fixed data set (4 CNY accounts,
  ~3,400 transactions) and skips onboarding. It exists only in benchmark builds.
- `.github/workflows/perf-device.yml` runs `:benchmark` on a physical phone in Firebase
  Test Lab (default Pixel 10 Pro, `model=blazer,version=36`). Needs the
  `FIREBASE_SERVICE_ACCOUNT` secret. Physical-device time is billed, so it only runs when
  started by hand (Actions → Performance (device) → Run workflow, on the branch to
  measure); nothing in a commit message triggers it.
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
- Run inputs: `mode` is `benchmark` (the A/B), `profile` (see Baseline Profile) or `apks`
  (only build: the `apk-base` and `apk-candidate` artifacts hold benchmark APKs that
  install side by side); `base_ref` picks the baseline (default `main`); `tests` names
  `ScreenBenchmark` methods to run instead of all; `compose_trace` adds composable names
  to the traces (benchmark builds carry `runtime-tracing`; it slows composition, so use it
  to diagnose, not to compare).
- `flingDownAndUp` swipes with UiObject2's fling gesture but not `fling()` itself: that waits
  5 s for a scroll-finished event Compose lists never send. It sleeps while the list coasts.
- Baseline Profile: `app/src/main/baseline-prof.txt`, installed on sideloaded builds by
  `profileinstaller`. A run with `mode: profile` regenerates it: `BaselineProfileGenerator` runs on the
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
  list's first draw; exact, and adds no work to the app. UiAutomator only drives the
  test (taps, flings, waiting for rows); it times nothing, since each lookup adds its own
  overhead. Baselines older than the trace section report no data for it.
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

## Lint

`app/lint.xml` plus the `lint {}` block in `app/build.gradle.kts`:

- Default English and `values-zh` are the maintained locales
- `values-zh-rTW` has Traditional Chinese for the new institution/settings strings
- `MissingTranslation` is ignored so stale Crowdin locales do not fail the build
- Dependency-version lint is disabled so builds are not blocked by upstream catalog drift
- `checkReleaseBuilds` is off; debug lint is `./gradlew :app:lintStandardDebug`
