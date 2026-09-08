# Home Recent loading and scroll spikes

## Observed failure and state fix

The connected phone showed a persistent Recent spinner after other Home data had
loaded. `refreshHiddenAccounts()` evaluated `_uiState.value.copy(...)` before a
suspending `netWorthIn()` argument. If recent transactions finished during that
suspension, the subsequent assignment could overwrite them and restore the old
`isLoading=true` value. Several other Home state writes were also non-atomic.

Changes:
- All Home state merges now use `MutableStateFlow.update` with non-suspending
  reducers. Monthly change is calculated from one current state snapshot.
- Remove the duplicate startup hidden-account refresh. Its public method delegates
  to the existing account refresh, which computes totals before merging state.
- Recent uses a Room query with eligibility filtering and SQL LIMIT, instead of
  materializing every transaction and taking three in Kotlin. No schema migration.
- Publish local rows before optional currency enrichment. Enrichment is cancellable
  and bounded to 2 seconds; missing/failed rates leave native amounts/currencies
  visible. The conversion target is stored alongside its amounts to avoid relabeling.
- A query failure stops loading and exposes Retry; retry preserves any cached rows.

## Scroll work

Removing the page-wide shared-transition wrapper alone did **not** demonstrate a
stable scroll improvement: the first controlled R8 comparison still had P95 near
10ms. Further tracing showed a widget prefetch composition up to 26.83ms and an
idle-prefetch slice up to 37.83ms.

The Home heatmap previously composed 182 clipped cell Boxes plus 26 Columns. It now
uses one Canvas, caching date/color calculations and matching the previous per-cell
pixel rounding. Month labels follow the same rounded cell step. Pixels, color
buckets, and updates are covered by a rendering test.

Home widget items now have distinct content types, avoiding reuse of a heavy widget
slot for a structurally different widget. Home is no longer wrapped in a page-wide
SharedTransitionLayout. Existing budget/loan controls retain small local scopes;
unmatched cross-route bounds on Home search/subscription/View All controls are gone.

## Device checks

Only the separate `com.ritesh.cashiro.lagcheck.debug` sandbox was installed/replaced
and seeded. The user's bookkeeping APK and data were not replaced or modified.
The test package is guarded against seeding any other package.

Actual integration tests run MainActivity → HomeScreen → Hilt HomeViewModel →
Room/DataStore, with 1000 eligible transactions, eight accounts and a cached USD/CNY
rate. They cover cold launches, profile updates, scrolling, no persistent spinner,
SQL limit, and exclusion of deleted/balance-update records.

- 256 JVM tests passed (including six new Recent flow tests: delayed/never-returning
  rates, timeout, currency cancellation, query failure, no rate, and rate refresh).
- 21 isolated real-device tests passed, including the full Home integration,
  Home scroll bounds, heatmap pixel checks, prior navigation and row regressions.
- Standard/F-Droid debug compilation and Android test-source compilation passed.

## R8 comparison

Pixel 9 Pro Fold, outer screen, Android 17, dark mode, floating navigation with blur
on. Both APKs used `-PslimDebug` and the same fixture configuration. Three rounds per
build used six short 250px/500ms gestures each; this avoids spending most of the
sample at the scroll boundaries. Thermal Status was 0 at the checkpoints. Fixture
data was rebuilt by integration tests using the same counts, values, currencies,
and layout; this is not a comparison against the user's private ledger.

| Build / round | Frames | Android janky frames | P95 | P99 |
| --- | ---: | ---: | ---: | ---: |
| Baseline 1 | 395 | 4 | 10ms | 53ms |
| Baseline 2 | 411 | 4 | 10ms | 19ms |
| Baseline 3 | 412 | 4 | 10ms | 21ms |
| Final 1 | 414 | 2 | 9ms | 12ms |
| Final 2 | 415 | 1 | 9ms | 12ms |
| Final 3 | 412 | 0 | 10ms | 13ms |

Aggregate slow frames: 12/1218 versus 3/1241. These small samples support reduced
occasional spikes in this fixture; they do not establish a universal FPS gain or
promise smoothness for every ledger/device. P95 changed only slightly. Initial
long-gesture trials and intermediate builds are retained in local logs, not mixed
into this table.

Reproducible commands, build outputs, tests, APK hashes, traces and frame statistics
are in ignored `build/home-lag/`. The isolated init script changes only the test
application ID; it does not alter the shipped build configuration. Screenshots and
raw phone traces stay local and are not published to the PR.
