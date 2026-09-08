#!/usr/bin/env python3
"""Source-level motion budget guard, not a frame-time benchmark. No Android SDK required."""
import argparse
import json
import re
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
parser.add_argument("--check", action="store_true", help="Fail if the quiet UI invariants regress")
args = parser.parse_args()
base = args.root / "app/src/main/java/com/ritesh/cashiro"

def read(path):
    return (base / path).read_text()

art = [
    "presentation/ui/components/TiledScrollingIconBackground.kt",
    "presentation/ui/components/BudgetCard.kt",
    "presentation/ui/features/categories/IconSelector.kt",
    "presentation/ui/features/profile/ProfileScreen.kt",
    "presentation/ui/features/settings/about/AboutScreen.kt",
]
visibility = read("presentation/effects/BlurredAnimatedVisibility.kt")
icons = read("presentation/ui/features/categories/IconSelector.kt")
home = read("presentation/ui/features/home/HomeScreen.kt")
tabs = read("presentation/navigation/MainTabMotion.kt")
all_ui_source = "\n".join(p.read_text() for p in (base / "presentation").rglob("*.kt"))
metrics = {
    "implicit_size_animations": all_ui_source.count(".animateContentSize("),
    "custom_edge_translations": len(re.findall(r"\.overScrollVertical\((?:false)?\)", all_ui_source)),
    "custom_fling_handlers": len(re.findall(r"rememberOverscrollFlingBehavior\s*\{", all_ui_source)),
    "large_animated_content_wrappers": all_ui_source.count("AnimatedContent("),
    "unbounded_marquees": all_ui_source.count("iterations = Int.MAX_VALUE"),
    "decorative_infinite_transition_producers": sum(read(p).count("rememberInfiniteTransition(") for p in art),
    "visibility_legacy_blur_allocators": sum(visibility.count(token) for token in ["RenderScript.create(", "Bitmap.createBitmap(", "Allocation.createFromBitmap("]),
    "home_intercepts_back": "BackHandler {" in home,
    "icon_grid_is_lazy_and_typed": "LazyVerticalGrid(" in icons and 'contentType = { "icon" }' in icons,
    "peer_tabs_have_no_screen_transition": "if (isPeerSwitch()) EnterTransition.None" in tabs and "if (isPeerSwitch()) ExitTransition.None" in tabs,
    "blur_is_opt_in": "preferences[PreferencesKeys.BLUR_EFFECTS] ?: false" in read("data/preferences/UserPreferencesRepository.kt"),
}
print(json.dumps(metrics, indent=2, sort_keys=True))
if args.check:
    for key in ["implicit_size_animations", "custom_edge_translations", "custom_fling_handlers", "large_animated_content_wrappers", "unbounded_marquees"]:
        assert metrics[key] == 0, key
    assert metrics["decorative_infinite_transition_producers"] == 0
    assert metrics["visibility_legacy_blur_allocators"] == 0
    assert not metrics["home_intercepts_back"]
    assert all(metrics[k] for k in ["icon_grid_is_lazy_and_typed", "peer_tabs_have_no_screen_transition", "blur_is_opt_in"])
    print("PASS: quiet UI source invariants")
