# MyTube
Privacy first approach to navigating Youtube subscriptions without algorithms & shorts. Curate your Youtube feed privately without a Google account. Click out to your default browser to play videos. Unsubscribe with a single click.

MyTube fetches channel and video data directly from YouTube's own servers (via [NewPipeDownloader](https://github.com/teamnewpipe/newpipe)) and hands off to your browser or the YouTube app to actually play anything — it never signs in and never talks to any server of ours. That still means YouTube's servers see your IP address and which channels you're browsing, the same as visiting youtube.com while logged out. For stronger protection, recommend using a VPN alongside this app.

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
