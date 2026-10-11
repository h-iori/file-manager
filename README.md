# Files — by IORI STUDIOS

> High-performance luxury Android File Manager with multi-category storage analytics, interactive directory drill-down explorer, and modern glassmorphism aesthetics.

---

## Overview

**Files** is a modern, high-performance Android file manager crafted with Jetpack Compose, Material 3, and custom liquid glassmorphism design tokens. Built for seamless storage navigation, deep storage analytics, and elegant file operations.

---

## Features

- **Cyberpunk / Sci-Fi Brand Header**: Orbitron 3D isometric extrusion title with diffuse neon aura glow and signature "by IORI STUDIOS" branding.
- **Dynamic 360° Storage Pie Ring**: Complete 8-category storage breakdown dynamically visualizing total device storage:
  - 📷 **Photos** (Cyan)
  - 🎥 **Videos** (Purple)
  - 🎵 **Audio** (Lime)
  - 📄 **Documents** (Pink)
  - 📥 **Downloads** (Amber)
  - 📱 **Apps** (Deep Orange)
  - ⚙️ **System Occupied Space** (Slate Gray)
  - 🟢 **Available Space** (Emerald Green)
- **Interactive Directory Drill-Down Explorer**:
  - Full breadcrumb navigation trail (`Internal Storage > Folder > Subfolder`).
  - Smooth backstack management and hardware back button support.
  - Sorting options (Name A-Z, Name Z-A, Size, Date modified).
- **Beautified 3-Dot Actions & Quick Operations**:
  - File details inspection dialog with clean Outfit typography and file metadata.
  - Contextual operations: Open, Share, Details, Delete.
- **Strict Industry-Standard Typography**:
  - Orbitron for signature brand identity headers.
  - Outfit for all body text, cards, dialogs, and metrics.
  - Unabbreviated metric units (`GB`, `MB`, `KB`, `B`).
- **All Files Access**: Seamless Android 11+ (`MANAGE_EXTERNAL_STORAGE`) scoped and external storage handling.

---

## Tech Stack & Architecture

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: Modern Android Architecture (MVVM, StateFlow, Coroutines)
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`)
- **Min SDK**: 26 (Android 8.0)
- **Target / Compile SDK**: 35 (Android 15)

---

## Project Structure

```text
file-manager/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/ioristudios/filemanager/
│   │   │   │   ├── data/
│   │   │   │   │   ├── FileModels.kt          # Data models & categories
│   │   │   │   │   └── StorageScanner.kt      # Disk I/O & MediaStore engine
│   │   │   │   ├── theme/
│   │   │   │   │   ├── Color.kt               # Luxury Glassmorphism & chart palette
│   │   │   │   │   ├── Theme.kt               # Dynamic Dark Theme
│   │   │   │   │   └── Type.kt                # Orbitron & Outfit typography
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/
│   │   │   │   │   │   └── GlassComponents.kt # Donut chart, glass cards, menus
│   │   │   │   │   ├── directory/
│   │   │   │   │   │   └── DirectoryScreen.kt # Directory explorer & breadcrumbs
│   │   │   │   │   └── main/
│   │   │   │   │       ├── MainScreen.kt      # Home screen & top brand header
│   │   │   │   │       └── MainScreenViewModel.kt # State management & operations
│   │   │   │   └── MainActivity.kt            # Entry activity & permissions
│   │   │   └── res/
│   │   │       ├── font/                      # Orbitron & Outfit font assets
│   │   │       └── ...
│   │   └── ...
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Building and Installing

### Prerequisites
- Android Studio Ladybug / Koala or Android SDK Command-Line Tools
- JDK 17+

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Build & Install Release
```bash
./gradlew installRelease
```

---

## License

Developed with ❤️ by **IORI STUDIOS**.
