# LineageLauncher (Trebuchet) 🚀

[![Android CI & Release Build](https://github.com/rhythmcreative/lineage-launcher/actions/workflows/android-build.yml/badge.svg)](https://github.com/rhythmcreative/lineage-launcher/actions/workflows/android-build.yml)
[![Release](https://img.shields.io/github/v/release/rhythmcreative/lineage-launcher?style=flat&color=34A853)](https://github.com/rhythmcreative/lineage-launcher/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android%2010%2B%20(API%2029%2B)-242A32)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

A modern, standalone Android home screen launcher based on **LineageOS Launcher3 (Trebuchet)**. Designed with clean **Material 3 monochrome aesthetics**, a fluid **multi-page scrollable dock**, **dynamic drag-to-new-page expansion**, and an **integrated in-app auto-updater** powered by GitHub Releases.

---

## ✨ Features

### 📱 1. Multi-Page Scrollable Dock (Hotseat)
* **Paginated Dock Navigation:** Seamlessly swipe left and right across multiple dock pages.
* **Dynamic Drag-to-New-Page:** Drag an application icon to the dock's edge to automatically create a new empty dock page after 450ms of hover.
* **Infinite Looping:** Option to wrap continuously from the last dock page back to the first.
* **Adaptive Monochrome Indicators:** Material 3 high-contrast page dots with protective halos that remain clear and legible across all wallpaper shades.

### 🎨 2. Pure Blanco y Negro (B&W) & Material You Animations
* **Monochrome Themed Previews:** Vector animations inside settings rendered in pure monochrome (zero harsh color tints, zero blue dots).
* **Motion Assist Vector Animations:** Built-in vector drawing engine reproducing the Pixel Motion Assist aesthetic.
* **Lottie Swipe Gesture Showcase:** Includes the official Lottie interactive animation (`dock_swipe.json`) illustrating fluid horizontal gestures.

### 🔄 3. Built-in In-App Auto-Updater
* **GitHub Releases Integration:** Directly queries `https://api.github.com/repos/rhythmcreative/lineage-launcher/releases/latest`.
* **One-Tap Upgrade:** Detects new releases, downloads the signed `.apk`, and launches the Android Package Installer via `FileProvider`.
* **Automatic Background Checks:** Option in settings to check for new updates upon launch.

### ⚡ 4. Lightweight & Standalone
* Standalone Gradle architecture (`compileSdk = 35`, `minSdk = 29`).
* Builds via **GitHub Actions CI/CD** on every commit and tag.
* Compatible with any Android 10+ device (Pixel, LineageOS, AOSP, Samsung, etc.).

---

## 🛠️ Building from Source

### Prerequisites
* JDK 17 or higher
* Android SDK (API 35)

### Build Debug APK
```bash
./gradlew assembleDebug
```
The resulting APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Build Release APK
```bash
./gradlew assembleRelease
```
The resulting APK will be located at:
```
app/build/outputs/apk/release/app-release.apk
```

---

## 📦 Releases & CI/CD Workflow

Every tag pushed to GitHub (e.g. `v1.0.0`) automatically triggers the GitHub Actions workflow, compiling the APK and publishing a new release with downloadable assets.

The launcher automatically detects these releases through its integrated `LauncherUpdater`.

---

## 📄 License
Licensed under the Apache License, Version 2.0 (the "License"). You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0).
