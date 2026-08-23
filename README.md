# NoSnooze

A task-locked Android alarm for people who find waking up difficult. NoSnooze has no in-app snooze or dismiss action: the ringing screen stops only after the selected wake-up mission succeeds.

## Wake-up missions

1. **Photo mission (default)** — photograph any fixed place or object you choose: a balcony, bathroom sink, coffee machine, front door, or something else away from bed. When the alarm rings, return there and take a matching live photo. A brightness-normalized structural comparison is designed to tolerate changes between daytime and nighttime lighting.
2. **Read out loud** — clearly read the two lines shown by the app. Android's on-device/system speech recognizer checks word coverage.
3. **Awake selfie** — take a front-camera selfie. ML Kit face detection checks that a face is centered and both eyes are open.

Photos are kept in private app storage, alarm data is held in Room, preferences use DataStore, and nothing is uploaded by NoSnooze.

## Alarm behavior

- `AlarmManager.setAlarmClock` provides exact scheduling and Android's status-bar alarm indicator.
- A high-priority alarm notification launches a lock-screen, screen-on mission activity.
- A foreground service loops the selected system alarm tone and owns a configurable vibration waveform.
- Enabled alarms are restored after reboot, clock changes, and timezone changes.
- One-time and weekly repeating alarms are supported.
- Alarm sound, volume, gradual volume, and vibration intensity are configurable.

> Android always allows the device owner to force-stop an app, revoke permissions, or power off the phone. No third-party app can securely prevent those system actions. NoSnooze intentionally removes only ordinary in-app dismissal.

## Technology

- Native Kotlin
- Jetpack Compose + Material 3
- Room and Preferences DataStore
- CameraX
- ML Kit face detection
- AlarmManager, foreground service, MediaPlayer, and Vibrator APIs
- Min SDK 26 / Target SDK 35

## Build

Open the project in a current Android Studio with JDK 17 and Android SDK 35 installed, or run:

```bash
./gradlew assembleDebug
```

The repository's lightweight Gradle bootstrap downloads Gradle 8.9 on first use. Install the debug APK from `app/build/outputs/apk/debug/app-debug.apk` on a physical Android device; alarm, camera, microphone, vibration, and lock-screen behavior cannot be fully evaluated in a basic emulator.

On first launch, grant notification and exact-alarm access. Android 14+ may also require enabling **Full-screen alerts** from **Alarm power → Phone access**. Camera or microphone access is requested only when its mission needs it.

## Project layout

- `data/` — Room alarm model/DAO and local settings
- `alarm/` — exact scheduling, reboot restoration, notification, receiver, and ringing service
- `ui/` — Compose home/editor/settings and lock-screen challenge UI
- `util/ImageSimilarity.kt` — local, lighting-tolerant photo comparison
