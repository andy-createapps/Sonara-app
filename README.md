# Sonara

Sonara is a music app for Android, built on [BitChord](https://github.com/kushagrasinghx/BitChord)
by kushagrasinghx, which is licensed under the GNU GPL v3. This is a modified version:
renamed and rebranded, with its own icons, no automatic app updates, a per-part update
section in Settings, and a second build with a welcome quiz and a few hidden extras.

Sonara is free software under the **GNU General Public License v3.0**. See `LICENSE`.
The original copyright notices stay in the source files. Anyone who receives a Sonara APK
can ask for this source code and may pass it on under the same license.

## Two builds

The same code makes two apps, chosen with a Gradle property:

| Build | Command | App id |
| --- | --- | --- |
| Andy's | `./gradlew assembleProdRelease -Pedition=andy` | `com.sonara.app` |
| Camellia's | `./gradlew assembleProdRelease -Pedition=camellia` | `com.sonara.app.c` |

They have different app ids, so both can be installed on one phone.

## Building on GitHub

Push this folder to a GitHub repository. The workflow in `.github/workflows/android.yml`
runs on every push (or from the Actions tab, "Run workflow"), and attaches
`Sonara-Andy.apk` and `Sonara-Camellia.apk` to the run under **Artifacts**.

Optional: add `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` as
repository secrets so every build is signed with the same key. Without them each build gets
a fresh key, and a new build can't be installed over an old one (uninstall first).

## Updates

Sonara never checks for a new version of itself. Settings > Components checks the
YouTube extractor, sources and addons, lyrics and Spotify import separately.
