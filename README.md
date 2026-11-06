# VolumeFader

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android_8.0+-green.svg)](https://developer.android.com)

**VolumeFader** is a lightweight, background-resilient sleep timer application for Android that smoothly attenuates media volume to zero across a user-defined duration.

---

## Technical Overview

Most default sleep timers on Android either stop media abruptly at the end of a countdown or step the volume linearly across discrete hardware levels, creating sudden drop-offs at low levels due to human auditory perception. 

VolumeFader addresses this by:
1. **Perceptual Loudness Modeling:** Providing mathematical interpolation curves (Logarithmic/dB, Linear, and Exponential) that mitigate the perceived volume drop between low stream steps.
2. **Reliable Background Execution:** Operating within an Android Foreground Service with low-power wake-lock safeguards against aggressive Doze mode CPU throttling.
3. **Playback Clean-Up:** Optionally dispatching a global media pause key event (`KEYCODE_MEDIA_PAUSE`) once the volume hits zero, ensuring background streaming services do not drain battery overnight.

---

## Fade Curves & Mathematical Modeling

Given duration $T$, elapsed time $t \in [0, T]$, and normalized progress $p = \frac{t}{T} \in [0, 1]$, the remaining factor $r = 1 - p \in [1, 0]$ is mapped to volume factor $f(r) \in [0, 1]$:

### 1. Perceptual Loudness (Logarithmic / dB Compensation)
Human auditory sensation scales logarithmically with sound pressure level (Weber-Fechner law). A linear decrease in hardware steps causes perceived volume to plunge too quickly at the tail. The perceptual curve maintains gradual attenuation at lower levels:

$$f_{\text{perceptual}}(r) = \frac{\ln(1 + 9r)}{\ln(10)}$$

### 2. Linear
A steady, proportional reduction across elapsed time:

$$f_{\text{linear}}(r) = r$$

### 3. Exponential
A decay profile that drops quickly from loud baseline levels and spends the majority of the duration in low, unobtrusive ambient levels:

$$f_{\text{exponential}}(r) = r^2$$

---

## Architecture

The project follows standard Android Jetpack architecture principles with single-responsibility modularity:

```
app/src/main/java/com/carlomontec/volumefader/
├── MainActivity.kt               # Jetpack Compose UI container & permission handling
├── audio/
│   ├── AudioVolumeManager.kt     # Low-level AudioManager interface & silent stream stepping
│   ├── FadeCurve.kt              # Attenuation curve implementations
│   └── MediaPlaybackController.kt# MediaSession / KEYCODE_MEDIA_PAUSE broadcaster
├── model/
│   └── FadeModels.kt             # Domain configurations and reactive status models
├── service/
│   └── VolumeFadeService.kt      # Foreground Service, Notification management & tick loop
└── ui/
    ├── screens/
    │   └── MainFaderScreen.kt    # Compose interface, presets, slider, and live timer
    └── theme/
        ├── Color.kt
        └── Theme.kt              # High-contrast, dark-first sleep palette
```

---

## Building and Development

### Prerequisites
* Android Studio Ladybug (2024.2.1+) or newer
* Android SDK 35 (compileSdk: 35, minSdk: 26)
* JDK 17+

### Compiling via Gradle
```bash
# Clone the repository
git clone git@github.com:carlomontec/VolumeFader.git
cd VolumeFader

# Run unit tests
./gradlew test

# Assemble debug APK
./gradlew assembleDebug
```

---

## Author

* **Carlo Monjaraz-Tec**

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details.
