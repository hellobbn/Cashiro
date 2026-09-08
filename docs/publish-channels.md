# GitHub publishing and update channels

Each fetch uses one channel and never falls back to another. Debug builds can only follow debug. Release and testing builds can switch between the **Release** and **Testing** lanes in About.

| Build | Application ID | Signer | GitHub source | Update ordering | Filename |
| --- | --- | --- | --- | --- | --- |
| Debug | `com.ritesh.cashiro.debug` | Persistent debug keystore | `releases/tags/debug-latest` | `commit_count` | `Cashiro-debug-cN-sha-abi.apk` |
| Testing | `com.ritesh.cashiro` | Same `KEYSTORE` as release | `releases/tags/testing-latest` | `commit_count` | `Cashiro-testing-cN-sha-abi.apk` |
| Release | `com.ritesh.cashiro` | `KEYSTORE` | `releases/latest` (stable only) | `version_code` | `Cashiro-vVERSION-abi.apk` |

- Debug: pushes to `main` run **Debug APK**, sign with the persistent debug identity, and publish the rolling `debug-latest` prerelease with `make_latest: false`.
- Testing: the **Testing APK** workflow is **manual** (`workflow_dispatch` only). It builds `assembleStandardPreview` (AGP forbids build type names that start with `test`; the GitHub / in-app channel is still `testing`). The APK is minified and release-class, signed with the same `KEYSTORE` environment as release, and published as the rolling `testing-latest` prerelease with `make_latest: false`. Filenames include git commit count and short hash.
- Release: the **Release** workflow runs on `v*` tags or manual dispatch. Before publishing, update `versionName` and increment `versionCode`; the tag must match `versionName`. Release signing uses the existing `KEYSTORE` environment, not debug secrets.
- Testing APKs keep `applicationId` `com.ritesh.cashiro` so they install over an existing release or testing install. Their Android `versionCode` is the git commit count, which is higher than official `versionCode` values. Installing a later official release with a smaller `versionCode` over a testing install is blocked by Android; uninstall first, or keep following testing.
- Stable releases become GitHub's latest release. Testing and debug tags stay prerelease and never become the stable update source.
- Release notes include machine-readable `version_code:`. Testing and debug notes include `commit_count:`. Older releases without the required metadata produce a failed update check rather than a misleading up-to-date result. Do not remove this metadata when editing release notes.
- A missing channel, incompatible ABI, draft, or wrong-channel asset fails the update check; it does not offer a different package. ABI-specific APKs are preferred; universal APKs are the only architecture fallback.
- Dismissed builds are stored per channel (debug / testing / release).

`dry_run: true` previews release metadata only; it does not build, sign, create a tag, or publish a release. No automatic release dispatch is needed for debug or testing updates.
