# MyTube
Privacy first approach to navigating Youtube subscriptions without algorithms & shorts. Curate your Youtube feed privately without a Google account. Clicking out to your default browser to play videos and unsubscribing is as simple as removing a channel from your subscriptions list.

## Installation

MyTube isn't on the Play Store — it's distributed as a signed APK attached to each [GitHub Release](https://github.com/VaranauskasD/MyTube/releases).

### Option A: Obtainium (recommended)

[Obtainium](https://obtainium.imranr.dev/) tracks this repo's GitHub Releases and lets you install and auto-update MyTube without an app store.

1. Install Obtainium ([F-Droid](https://f-droid.org/packages/dev.imranr.obtainium/) or its [GitHub Releases](https://github.com/ImranR98/Obtainium/releases))
2. Open Obtainium → **Add App**
3. Paste this repo's URL: `https://github.com/VaranauskasD/MyTube`
4. Obtainium finds the latest release's APK automatically — install, and future updates surface the same way

### Option B: Manual APK

1. Download the `.apk` from the [latest release](https://github.com/VaranauskasD/MyTube/releases/latest)
2. Open it on your device (you'll need to allow installs from this source) and install

### Option C: Build from source

```bash
git clone https://github.com/VaranauskasD/MyTube.git
cd MyTube
./gradlew assembleDebug
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`. `./gradlew installDebug` builds and pushes it straight to a connected device over `adb`.

## Releases

Versioning follows [semver](https://semver.org). Each `vX.Y.Z` tag pushed to this repo triggers a GitHub Actions workflow that builds a signed release APK and publishes it as a GitHub Release — this is what Obtainium (and Option B above) consume.
