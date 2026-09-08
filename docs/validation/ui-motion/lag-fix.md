# Measured follow-up: tab routing and transaction row layout

This follows the real-device investigation of PR #6. It is a targeted correction,
not another app-wide animation pass.

## Navigation correction

Both normal and floating bottom navigation now use the same `navigateToMainTab`
implementation. Peer switches anchor to the persistent `Home` route rather than
`graph.findStartDestination()`.

The old graph can still identify `OnBoarding` as its start destination after
onboarding has been removed. On the reported phone, NavController repeatedly
logged an ignored pop to destination `-448775616` (the OnBoarding route), and saved
peer stacks could restore the wrong tab. The new anchor prevents that accumulation.
When a saved stack from the old behavior is already contaminated with other tabs,
the helper retains the requested root's state and removes the incorrectly restored
peers; it recreates the requested root only if it cannot be recovered.

Reselecting a root remains a no-op. Filtered/search transaction entries still reset
to the default transaction tab. Normal and floating navigation share all of this logic.

## Transaction row correction

The captured trace identified main-thread measurement/recomposition as a hotspot
(one `AndroidOwner:measureAndLayout` span was 19.16ms, including 7.94ms recomposition
and 3.81ms of text measurement). These numbers describe the pre-fix installed APK,
not a claimed post-fix speedup.

- Remove merchant-name marquee measurement; the existing bounded two-line layout
  and ellipsis now actually apply.
- Respect `showDate` and key formatted date text by locale/formatter as well as date.
  Today/Yesterday rows omit the redundant per-row date; This Week/Earlier retain
  dates because those groups may contain several calendar days.
- Replace decorative text bullets with drawing/layout-only dots.
- Render non-interactive metadata pills without Material Surface plumbing; iconless
  tags use a single Text with background/padding instead of extra containers.
- Construct joined subtitle strings only for card-style rows that consume them.
- Resolve an account once per list item instead of rebuilding its key three times.

No financial entity, schema, balance, or account data is modified.

## Verification

The user's installed bookkeeping application was not replaced. Device tests used
`com.ritesh.cashiro.lagcheck.debug` and its own test package/sandbox, selected through
a local-only Gradle init script. Those temporary packages were removed afterward.
No emulator was used for this follow-up.

Before the fix, on the isolated baseline APK:
- Existing Home-start navigation controls passed (2 tests).
- Both new onboarding-start navigation regressions failed: saved tab state was lost.
- Both row regressions failed: `showDate=false` still displayed a date, and the
  supposedly two-line merchant text was laid out as one unbounded marquee line.

After the fix:
- 15 targeted real-device tests passed: six navigation cases covering both styles,
  onboarding completion, and legacy saved-peer-stack repair; three row layout/tag
  checks; two existing list/sheet interaction tests; two selection-slot tests;
  and two LTR/RTL predictive-back commit/cancel tests.
- Standard and F-Droid debug Kotlin compilation passed.
- Standard debug unit tests passed (250 tests).
- Android test sources compiled successfully.

The row tests also cover changing locale, custom subtitle overrides, card-style
subtitles, long merchant names, unchanged displayed amounts, and click behavior.
The existing 1,000-row list test verifies scrolling and sheet dismissal preserve
position.

These tests establish the routing and display corrections, not a new whole-app
frame-time result. A repeat of the optimized CI APK's real-data scroll trace is
still needed to quantify the end-to-end performance change.

Exact commands, APK/source hashes, baseline failing outputs, final passing outputs,
and a hash-checked source rollback are retained locally in `build/lag-fix/`.
