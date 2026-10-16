# Jellyfin WebView Android

A lightweight, zero-bloat Android WebView wrapper for [Jellyfin](https://jellyfin.org/).

Designed to be built **100% in the cloud via GitHub Actions**—no local Android Studio, Android SDK, or Gradle installation required on your machine.

---

## Features

- **First-Launch Server Setup**: Enter your Jellyfin server URL (e.g., `http://192.168.1.100:8096` or `https://jellyfin.example.com`) once and it is saved locally on your device.
- **Persistent Login**: Cookies, `localStorage`, and `IndexedDB` are persisted so you stay logged into your Jellyfin server across app restarts.
- **Fullscreen Video Playback**: Supports HTML5 fullscreen video with automatic landscape orientation lock, hidden system bars (immersive mode), and `KEEP_SCREEN_ON` while watching media.
- **HTTP & HTTPS Support**: Works out of the box with both local HTTP IP addresses (`usesCleartextTraffic="true"`) and remote HTTPS domains.
- **Hardware Back Button Navigation**: Navigates backward through Jellyfin pages or exits fullscreen video cleanly.
- **Connection Recovery**: If the server is unreachable or changes IP, the setup screen automatically appears so you can update the server URL.

---

## Guide: Building on GitHub & Getting Your APK (Zero Local Disk Space)

Because this repository uses `.github/workflows/android-build.yml`, GitHub's cloud runners handle downloading Java, Gradle, and the Android SDK, compiling the app, and packaging the `.apk`.

### Step 1: Create a New Repository on GitHub
1. Go to [https://github.com/new](https://github.com/new).
2. Enter a repository name (for example: `jellyfin-webview-android`).
3. Choose **Public** (or **Private**) and click **Create repository** (do *not* initialize with a README or `.gitignore` since this project already has them).

### Step 2: Push This Project to GitHub
Open your terminal in this project folder and run:

```bash
cd ~/Documents/Projects/jellyfin-webview-android

git remote add origin git@github.com:albertlp-iq/jellyfin-webview-android.git
git push -u origin main
```

### Step 3: Download the Built APK from GitHub Actions
1. As soon as you push, go to your GitHub repository in the browser and click the **Actions** tab.
2. You will see the **Build Android APK** workflow running (takes ~1.5–2 minutes).
3. Once it turns green (**completed**), click on the workflow run.
4. Scroll down to the **Artifacts** section at the bottom of the page:
   - **`jellyfin-webview-release-apk`** — Optimized release build (signed and ready to install).
   - **`jellyfin-webview-debug-apk`** — Debug build.
5. Download the `.zip` artifact, extract `jellyfin-webview-release.apk`, transfer it to your Android phone/TV/tablet, and install it!

---

## Optional: Creating a Official GitHub Release Automatically

If you push a version tag starting with `v` (like `v1.0.0`), GitHub Actions will automatically create a **GitHub Release** and attach the `.apk` files directly to your repo's **Releases** page (no `.zip` extraction needed):

```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## Project Structure

```text
jellyfin-webview-android/
├── .github/
│   └── workflows/
│       └── android-build.yml        # Cloud CI/CD pipeline (JDK 17 + Gradle 8.7)
├── app/
│   ├── build.gradle.kts             # App configuration & dependencies
│   ├── proguard-rules.pro           # ProGuard rules
│   └── src/main/
│       ├── AndroidManifest.xml      # Permissions, cleartext HTTP, activity config
│       ├── java/com/jellyfin/webview/
│       │   └── MainActivity.kt      # WebView setup, fullscreen video, URL storage
│       └── res/
│           ├── drawable/            # Vector launcher icon
│           ├── layout/              # Main WebView + Server URL setup screen
│           └── values/              # Strings, colors, dark theme
├── build.gradle.kts                 # Root Gradle plugins
├── gradle.properties                # AndroidX & JVM settings
└── settings.gradle.kts              # Repository definitions
```

---

## Changing the Saved Server URL Later

If your Jellyfin server IP changes and the old server is unreachable, the app will automatically display the server URL prompt again. You can also clear app data in **Android Settings → Apps → Jellyfin WebView → Storage → Clear Data** at any time to reset the server URL.
