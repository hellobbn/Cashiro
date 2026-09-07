# GitHub publishing and update channels

Channels are fixed at build time by `BuildConfig.UPDATE_CHANNEL`; the updater never falls back to the other channel.

| Build | Application ID | GitHub source | Update ordering |
| --- | --- | --- | --- |
| Debug | `com.ritesh.cashiro.debug` | `releases/tags/debug-latest` | `commit_count` |
| Release | `com.ritesh.cashiro` | `releases/latest` (stable only) | `version_code` |

- Debug: pushes to `main` run **Debug APK**, sign with the persistent debug identity, and publish the rolling `debug-latest` prerelease with `make_latest: false`.
- Release: the **Release** workflow runs on `v*` tags or manual dispatch. Before publishing, update `versionName` and increment `versionCode`; the tag must match `versionName`. Release signing uses the existing `KEYSTORE` environment, not debug secrets.
- Stable releases become GitHub's latest release. Prereleases do not become the stable update source; install them manually if desired. For manual publishing, set the prerelease option appropriately.
- Release notes include machine-readable `version_code:`. Older releases without it produce a failed update check rather than a misleading up-to-date result. Do not remove this metadata when editing release notes.
- A missing channel, incompatible ABI, draft, or wrong-channel asset fails the update check; it does not offer a different package. ABI-specific APKs are preferred; universal APKs are the only architecture fallback.
- Dismissed debug commit counts and release version codes are stored separately.

`dry_run: true` previews release metadata only; it does not build, sign, create a tag, or publish a release. No automatic release dispatch is needed for debug updates.
