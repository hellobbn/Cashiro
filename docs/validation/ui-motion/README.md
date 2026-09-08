# UI motion and rendering cost review

Subsequent measured corrections are documented in [tab routing and row layout](lag-fix.md)
and [Home loading and scroll spikes](home-loading-fix.md).
The inventory and compile-only validation below describe the initial `1ad3500a` pass.
The follow-up has separately authorized, isolated real-device tests.

## Initial motion-pass scope

82 production Kotlin paths: 80 modified, one added (`OptionalHazeSource.kt`),
one removed (`Overscroll.kt`). These are code-inspection findings and reductions
in unnecessary rendering work, not measured frame-time improvements.

The pass covers Home, Analytics, transactions/details/editing, accounts,
subscriptions, budgets/history/editing, categories/icon selection, contacts,
lending/borrowing, profile, settings, backup/restore, webhooks, shared controls,
sheets, navigation and scrolling primitives.

## Changes and suspected costs

| Area | Change | Cost avoided |
| --- | --- | --- |
| Implicit size animation | 35 `animateContentSize` attachments removed | Repeated whole-container measurement; nested size animations in forms/cards/sheets |
| Scroll effects | 31 custom edge translations and 21 custom fling handlers removed | Extra nested-scroll dispatch, per-frame bounds checks, spring settling and translation layers on top of native scrolling |
| Backdrop capture | 38 source sites reviewed; one duplicate banner capture removed; remaining 37 use the theme-gated helper | Capturing scrolling content even when no blur is requested |
| Blur visibility | Removed RenderEffect/bitmap/RenderScript paths; short alpha-only defaults | Extra offscreen passes and legacy resource allocations during visibility changes |
| Blur defaults | Absent preference and initial theme states default off; explicit saved choices still win | Initial blur flash and default backdrop processing |
| Continuous decoration | Five infinite-transition producers removed | Moving budget meshes, scrolling tiled artwork, badge rotation and pulsing icon glows |
| Search hints | Four decorative timers removed, including one unused timer in transaction entry | Periodic recomposition and hint transitions while otherwise idle |
| Large subtree swaps | Eight `AnimatedContent` wrappers removed | Overlapping old/new icon grids, category rows, backup and webhook forms plus size transforms |
| Icon picker | Per-icon adaptive lazy grid; stable keys/content types; deduplicate resource aliases after search | Eager composition of whole categories and duplicate lazy keys; alias search retained |
| Chart display | Static line stroke/gradient and balance reveal; immediate bar updates; 120ms pie-selection feedback | 1.5s chart redraws, delayed gradient passes, low-stiffness bouncy bars; line models are now remembered |
| Monetary text | Format exact decimal input once, without counting from zero | Per-frame formatting/text measurement/font resizing, intermediate amounts and Float precision loss |
| Budget progress | No zero-to-value entrance; 150ms real-value updates read in draw phase | Entire-card recomposition and width remeasurement during each progress frame |
| Bulk selection/loading | Immediate checkbox colors/checkmark and loading dim state | Three animated states per checkbox; three redundant totals-alpha animations |
| Small feedback | Switcher offset, chevrons and drag elevation read state in placement/layer phases | Recomposition of surrounding form/card/widget content on each animation frame |
| Text marquee | All 24 sites limited to one pass; eight explicit unbounded loops removed | Indefinite scrolling text in lists, balances and rules; long content can still be revealed |
| Shadows/masks | Decorative manual shadows removed; drag shadow retained in its layer; two scrolling alpha masks removed | Shadow rendering and full-row offscreen compositing; shape clipping preserved |
| Navigation | Immediate peer tabs; 220ms RTL-aware detail transitions; bounded shared bounds; explicit bouncy springs removed | Stacked full-screen fade/scale/slide and long spring tails |
| Predictive back | Root Home no longer intercepts back for double-press exit | System now owns back-to-home preview/exit; selection dismissal handlers remain |
| Minor redundant work | Static toolbar offset; stable default haze state; constant spotlight alpha layer removed; tiled artwork clipped | Unneeded state/layers and drawing outside decoration bounds |

### Intentionally retained / remaining candidates

- Native scrolling, ripples, loading indicators, actual form disclosures, drag and
  pager gestures, sheet behavior, and short selection/progress feedback remain.
- Scoped shared-transition layouts remain because screen receiver APIs and
  explicit shared-element flows depend on them. Their lookahead measurement is
  still a candidate for a separate structural refactor, not a proven bottleneck.
- Optional blur still has a cost when explicitly enabled. This pass preserves the
  user's stored preference rather than silently overwriting it.
- Chart path construction, image decoding, and data-loading/recomposition outside
  motion can still affect scrolling; compile-only validation does not quantify them.

## Reference

Reviewed InstallerX Revived at `f6ffcd8ea84e629bc14160414e7343f07a4d3d76`:

- [HistoryPage](https://github.com/wxxsfxyzm/InstallerX-Revived/blob/f6ffcd8ea84e629bc14160414e7343f07a4d3d76/app/src/main/java/com/rosan/installer/ui/page/main/settings/history/HistoryPage.kt): stable record keys/content types without routine row entrances.
- [InstallerNavContainer](https://github.com/wxxsfxyzm/InstallerX-Revived/blob/f6ffcd8ea84e629bc14160414e7343f07a4d3d76/app/src/main/java/com/rosan/installer/ui/navigation/InstallerNavContainer.kt): navigation-owned transitions and optional effects.

This is an independent implementation on Cashiro's existing Navigation Compose,
not copied source or a migration to InstallerX's Miuix stack. Android's
[lazy-list](https://developer.android.com/develop/ui/compose/lists) and
[predictive-back](https://developer.android.com/develop/ui/compose/system/predictive-back)
guidance informed the adaptation.

## Initial-pass validation: compile only

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
./gradlew :app:compileStandardDebugKotlin :app:compileFdroidDebugKotlin \
  :app:compileStandardDebugAndroidTestKotlin --no-configuration-cache
```

Result: **BUILD SUCCESSFUL**, exit 0. Android test sources were compiled, not run.
The emulator was stopped when the user requested compile-only work; no subsequent
installation or device/emulator runtime validation was performed.

Earlier first-pass unit/emulator logs are historical and **do not validate the
expanded final changes**. Final compile output is `build/performance-review/wide-compile.log`.
Source inventory and a hash-checked rollback bundle are also in that ignored build
folder. No FPS, p95 latency or jank improvement is claimed.
