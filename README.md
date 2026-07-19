# OUCAi — On-Device AI Chat for Android

<p align="center">
  <img src="icon.png" alt="OUCAi Logo" width="128" height="128">
</p>

<p align="center">
  <strong>Run large language models locally on your Android device — no cloud, no internet required.</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#screenshots">Screenshots</a> •
  <a href="#quick-start">Quick Start</a> •
  <a href="#usage">Usage</a> •
  <a href="#project-structure">Project Structure</a> •
  <a href="#building">Building</a> •
  <a href="#dependencies">Dependencies</a> •
  <a href="#license">License</a>
</p>

---

## Features

- **🤖 On-Device Inference** — Run GGUF format LLMs directly on your phone using llama.cpp.
- **📥 Model Downloader** — Download curated models (DeepSeek, Llama) directly from Hugging Face.
- **📂 Local Model Import** — Import your own `.gguf` files via the system file picker.
- **💬 Chat Interface** — Full conversational UI with streaming token-by-token responses.
- **📜 Chat History** — Conversations are automatically saved and organized in a navigation drawer.
- **🎨 Customizable Theme** — Change primary and background colors with an interactive HSV color picker.
- **🌙 Dark Mode by Default** — Optimized for low-light use with full dark theme support.
- **📋 Logcat Viewer** — Built-in log viewer for debugging model output and app logs.
- **⚡ Model Benchmarking** — Built-in prompt processing and text generation benchmarks.
- **📊 GGUF Metadata Parser** — Automatically reads and displays model metadata on import.
- **🔄 Context Shifting** — Handles long conversations beyond the model's context window.

## Screenshots

| Chat Screen | Models Panel | Settings |
|:---:|:---:|:---:|
| *(screenshot placeholder)* | *(screenshot placeholder)* | *(screenshot placeholder)* |

## Quick Start

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 26+
- NDK 29.0.13113456
- A GGUF format model file (or use the built-in downloader)

### Build & Install

```bash
# Clone the repository
git clone https://github.com/ggml-org/llama.cpp.git
cd llama.cpp/examples/llama.android

# Open in Android Studio, sync Gradle, and run on your device
# Or build from command line:
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Usage

### Loading a Model

1. Open the app and tap the **Models** tab from the navigation drawer.
2. **Option A:** Tap a curated model (DeepSeek-R1 or Llama 3.2) and press **Download**.
3. **Option B:** Tap **"+ Add Model"** and select a `.gguf` file from your device.
4. Once downloaded/imported, tap **Load** on the model card.

### Chatting

- Type a message in the input bar and tap the **Send** button.
- Tap **Stop** (the send button turns into a stop icon) to halt generation.
- Use **New Chat** to start a fresh conversation.
- Use **More Options** → **Clear Conversation** to delete the current chat.

### Customizing Colors

1. Open the **Settings** panel from the navigation drawer.
2. Tap the color swatches next to **Primary Color** or **Background Color**.
3. Use the HSV picker and brightness slider to choose your color.
4. Tap **Apply** — the UI updates immediately.

### Viewing Logs

- Open the **Logcat** panel to see real-time Android logcat output filtered for the app.
- The **Model Log** tab shows GGUF metadata and model loading information.
- Use the play/pause, clear, and scroll buttons to control log output.

## Project Structure

```
llama.android/
├── app/                          # Android application module
│   ├── build.gradle.kts          # App-level build config
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/llama/
│       │   ├── MainActivity.kt           # Main UI and logic
│       │   ├── MessageAdapter.kt         # Chat message RecyclerView adapter
│       │   ├── ChatHistoryManager.kt     # JSON-based conversation persistence
│       │   ├── CuratedModel.kt           # Pre-defined model catalog
│       │   ├── ModelDownloadManager.kt   # HTTP model downloader
│       │   └── ColorPickerView.kt        # Custom HSV color picker View
│       └── res/                          # Layouts, drawables, themes, strings
├── lib/                          # Native inference library module
│   ├── build.gradle.kts          # CMake + NDK build config
│   └── src/main/cpp/
│       ├── ai_chat.cpp           # JNI bridge to llama.cpp
│       ├── CMakeLists.txt        # CMake build definition
│       ├── logging.h             # Android log wrapper
│       └── chat.h                # Chat template formatting
├── gradle/
│   └── libs.versions.toml        # Version catalog
├── settings.gradle.kts           # Multi-module project settings
├── build.gradle.kts              # Root build config
└── gradle.properties             # Gradle properties
```

## Building

### Native Library (CMake)

The native C++ library is built automatically via the Gradle CMake integration. Key CMake arguments defined in `lib/build.gradle.kts`:

| Argument | Value | Description |
|----------|-------|-------------|
| `BUILD_SHARED_LIBS` | `ON` | Build shared libraries |
| `LLAMA_BUILD_APP` | `OFF` | Disable llama.cpp app build |
| `LLAMA_BUILD_COMMON` | `ON` | Build common utilities |
| `GGML_NATIVE` | `OFF` | Disable native CPU optimizations |
| `GGML_CPU_ALL_VARIANTS` | `ON` | Build all CPU backend variants |
| `GGML_LLAMAFILE` | `OFF` | Disable llamafile support |

### Supported ABIs

- `arm64-v8a` — 64-bit ARM (most modern Android devices)
- `x86_64` — 64-bit x86 (emulators)

## Dependencies

### Android (Kotlin)
- **AndroidX** — Core KTX, Activity, Lifecycle, RecyclerView, DrawerLayout
- **Material Design 3** — Material Components (NavigationView, MaterialCardView, MaterialButton)
- **OkHttp** — HTTP client for model downloads
- **DataStore Preferences** — Key-value storage (used by native lib)

### Native (C++)
- **llama.cpp** — Core LLM inference engine
- **GGML** — Tensor library with CPU backend variants
- **JNI** — Java Native Interface bridge

## Technical Highlights

- **Streaming Generation:** Tokens are emitted one at a time via Kotlin coroutines (`Flow.collect`), providing real-time streaming output in the chat UI.
- **Context Shifting:** When the conversation exceeds the model's context window (8192 tokens), the native code discards the oldest half of the conversation history and shifts the remaining tokens, allowing arbitrarily long conversations.
- **Multi-Threading:** The native inference engine automatically selects 2–4 threads based on available CPU cores, with headroom to keep the UI responsive.
- **HSV Color Picker:** A custom `ColorPickerView` renders a full HSV color space using a cached bitmap with a brightness color matrix filter for smooth performance.

## License

This project is part of [llama.cpp](https://github.com/ggml-org/llama.cpp), which is licensed under the MIT License.

```
MIT License

Copyright (c) 2023-2024 The llama.cpp authors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<p align="center">
  Built with ❤️ using <a href="https://github.com/ggml-org/llama.cpp">llama.cpp</a>
</p>