# NoSnooze — Easy Technical Guide

**App:** NoSnooze  
**Platform:** Android 8.0 and newer  
**Language:** Native Kotlin  
**Current version:** 1.0.0 beta  

This document explains what NoSnooze does, which technologies it uses, why each technology is needed, how the files connect, how an alarm travels through the app, and how to build and release it.

---

## 1. The idea in simple words

NoSnooze is an Android alarm for people who find it difficult to wake up. A normal alarm has a dismiss or snooze button. NoSnooze deliberately does not provide those buttons while an alarm is ringing.

To stop the alarm, the user must finish the mission selected while creating it:

1. **Photo mission (default):** Go to a previously selected place or object and take a matching live photo.
2. **Read aloud:** Read the displayed passage clearly.
3. **Awake selfie:** Take a selfie with a visible face and both eyes open.

The alarm continues playing and vibrating until the mission succeeds.

> Android always lets the device owner force-stop an app, turn off the phone, revoke permissions, or uninstall it. No Android app can safely block these system controls. “Forceful” here means there is no ordinary dismiss or snooze button inside NoSnooze.

---

## 2. Technology overview

| Technology | What it is used for |
|---|---|
| **Kotlin** | The main programming language for all app logic |
| **Native Android** | Direct access to alarms, notifications, camera, microphone, vibration, lock screen, and local files |
| **Jetpack Compose** | Builds every screen and UI component in Kotlin |
| **Material 3** | Provides modern buttons, cards, switches, sliders, dialogs, and design rules |
| **Room** | Stores alarms in a local SQLite database |
| **Preferences DataStore** | Stores sound, volume, gradual-volume, and vibration settings |
| **AlarmManager** | Asks Android to trigger an alarm at an exact time |
| **setAlarmClock** | Marks it as a real alarm clock event and enables Android’s alarm indicator |
| **BroadcastReceiver** | Receives the alarm event and system restart/time-change events |
| **Foreground Service** | Keeps alarm sound and vibration running reliably in the background |
| **MediaPlayer** | Repeats the selected alarm sound |
| **Vibrator / VibrationEffect** | Produces configurable vibration patterns |
| **NotificationCompat** | Shows the urgent alarm notification and opens the full-screen mission |
| **CameraX** | Displays the camera and takes reference, matching, and selfie photos |
| **ImageSimilarity** | Compares a reference photo with a new photo locally |
| **Android SpeechRecognizer** | Converts spoken words into text for the read-aloud mission |
| **ML Kit Face Detection** | Finds a face and estimates whether both eyes are open |
| **Coroutines and Flow** | Runs database/image tasks away from the UI and updates screens automatically |
| **GitHub Actions** | Builds the APK online without requiring Android Studio on the owner’s computer |
| **Gradle** | Downloads dependencies and compiles/packages the Android app |
| **KSP** | Generates Room database code during the build |

---

## 3. Why Native Kotlin was selected

Native Kotlin is appropriate because an alarm app needs deep Android integration. NoSnooze directly uses:

- Exact alarm scheduling
- Android’s alarm-clock status indicator
- Lock-screen and screen-on behavior
- Foreground services
- Notification channels
- Camera and microphone permissions
- Boot-completed broadcasts
- Alarm audio and vibration APIs

A web app cannot reliably provide these features. Cross-platform tools can provide some of them through plugins, but Native Kotlin gives more direct control and fewer layers between the app and Android.

---

## 4. Main architecture

NoSnooze is separated into three main layers:

```text
Compose UI
   ↓ calls
ViewModel / Repository
   ↓ reads and writes
Room database + DataStore

AlarmManager
   ↓ triggers
BroadcastReceiver
   ↓ starts
Foreground ringing service + full-screen mission
   ↓ mission succeeds
Stop sound and vibration
```

### UI layer

Shows alarms, edits alarms, changes settings, and displays missions.

### Data layer

Stores alarm records and global alarm settings locally.

### Alarm/system layer

Schedules exact alarms, responds when Android triggers them, plays audio, vibrates, displays notifications, and restores alarms after restart.

---

## 5. Project folder map

```text
NoSnooze/
├── app/
│   ├── build.gradle.kts          App configuration and dependencies
│   └── src/main/
│       ├── AndroidManifest.xml   Permissions and Android components
│       ├── java/com/nosnooze/alarm/
│       │   ├── NoSnoozeApp.kt
│       │   ├── alarm/
│       │   ├── data/
│       │   ├── ui/
│       │   └── util/
│       └── res/                  Themes, colors, icons, and strings
├── .github/workflows/
│   └── build-apk.yml             GitHub cloud APK build
├── build.gradle.kts              Top-level build plugins
├── settings.gradle.kts           Project/module registration
├── gradle.properties             Gradle and AndroidX settings
├── gradlew                       Linux/macOS cloud build launcher
└── gradlew.bat                   Windows build launcher
```

---

## 6. Important Kotlin files

### `NoSnoozeApp.kt`

This is the application-level starting point. It creates:

- The Room database
- The DataStore settings manager
- The alarm notification channel

These objects are shared by the rest of the application.

### Data files

#### `data/Alarm.kt`

Defines one alarm record. Each record contains:

- Unique database ID
- Hour and minute
- User label
- Enabled/disabled state
- Repeating-day bit mask
- Selected challenge type
- Reference-photo path, when needed
- Creation time

It also defines the three challenge types:

```text
PHOTO_MATCH
READ_ALOUD
AWAKE_SELFIE
```

#### `data/AlarmDao.kt`

Room’s database access interface. It can:

- Watch all alarms
- Find one alarm by ID
- Read enabled alarms
- Save or update an alarm
- Delete an alarm
- Enable or disable an alarm

#### `data/AppDatabase.kt`

Defines the Room database and converts the challenge enum to and from text for SQLite storage.

#### `data/AlarmRepository.kt`

Sits between the UI and database. It ensures database changes and Android scheduling stay synchronized.

For example:

- Saving an enabled alarm also schedules it.
- Disabling an alarm also cancels its PendingIntent.
- Deleting an alarm cancels it before deleting its database row.

#### `data/SettingsStore.kt`

Uses DataStore for global settings:

- Selected sound URI
- Playback volume from 25% to 100%
- Gradual-volume option
- Vibration mode: off, steady, or intense

### Alarm files

#### `alarm/AlarmScheduler.kt`

Calculates the next date and time an alarm should ring.

It uses:

```kotlin
AlarmManager.setAlarmClock(...)
```

This is important because it:

- Treats the event as a real alarm clock
- Requests exact timing
- Can show Android’s alarm indicator in the status bar
- Gives the system information about the next alarm

If exact-alarm permission is unavailable, the code uses a less exact fallback so the alarm is not silently lost.

Repeating days are stored as seven bits:

```text
bit 0 = Sunday
bit 1 = Monday
bit 2 = Tuesday
bit 3 = Wednesday
bit 4 = Thursday
bit 5 = Friday
bit 6 = Saturday
```

A value of zero means the alarm rings once at its next occurrence.

#### `alarm/AlarmReceiver.kt`

Android calls this receiver when the scheduled time arrives.

It:

1. Reads the alarm ID.
2. Loads the alarm from Room.
3. Ignores it if disabled.
4. Disables a one-time alarm or schedules the next repeating occurrence.
5. Starts the ringing foreground service.

#### `alarm/AlarmRingingService.kt`

This is responsible for the actual ongoing alarm.

It:

- Starts as a foreground media-playback service
- Loads the selected sound and settings
- Repeats the sound with `MediaPlayer`
- Applies gradual volume when enabled
- Starts the selected vibration pattern
- Remains active if the mission activity is not in front
- Stops only when it receives the internal completion action

#### `alarm/AlarmNotifications.kt`

Creates the high-priority alarm notification.

The notification is:

- Ongoing
- Public on the lock screen
- Categorized as an alarm
- Connected to the full-screen `AlarmActivity`
- Missing any snooze or dismiss action

#### `alarm/RescheduleReceiver.kt`

Android does not permanently keep app alarms after every system event. This receiver reloads all enabled alarms after:

- Phone restart
- Manual clock change
- Timezone change
- Exact-alarm permission change

### UI files

#### `ui/MainActivity.kt`

The normal app entry point. It displays one of these pages:

- Home screen
- Alarm editor
- Settings

It also requests notification and exact-alarm access when required.

#### `ui/MainViewModel.kt`

Connects Compose screens to `AlarmRepository`. It exposes the alarm list as a reactive `StateFlow` and handles save, toggle, and delete operations.

#### `ui/AlarmActivity.kt`

The full-screen ringing activity.

It:

- Appears over the lock screen
- Turns the screen on
- Keeps the screen awake
- Blocks the normal Android back action inside the activity
- Loads the ringing alarm
- Displays its selected challenge
- Stops the service and closes only after success

If a photo reference is unexpectedly missing, it uses the read-aloud mission as a backup instead of trapping the user permanently.

#### `ui/screens/HomeScreen.kt`

Shows:

- The next enabled alarm
- All saved alarms
- Challenge type for each alarm
- Enable/disable switches
- Edit and delete controls
- New-alarm button

#### `ui/screens/AlarmEditorScreen.kt`

Lets the user choose:

- Time
- Alarm name
- Repeating days
- One of the three missions
- A photo reference for the default photo mission

It does not save a photo mission until a reference photo exists.

#### `ui/screens/SettingsScreen.kt`

Controls:

- Alarm volume
- Alarm sound selection
- Sound preview
- Vibration mode
- Gradual volume
- Exact-alarm settings shortcut
- Full-screen alert settings shortcut on Android 14+

#### `ui/screens/CameraCapture.kt`

A reusable CameraX screen. It can use:

- Back camera for a place/object
- Front camera for a selfie

It requests camera permission, displays a live preview, and saves a captured image into app-controlled storage.

#### `ui/screens/ChallengeCommon.kt`

Contains the shared alarm-mission header so all three missions look consistent.

---

## 7. How each mission works

## Mission 1: Photo match

### Setup

The user selects any fixed target away from bed, for example:

- Balcony
- Sink
- Coffee machine
- Front door
- Refrigerator

CameraX captures the reference photo and stores it in the app’s private files directory. The database stores only its local path.

### At alarm time

1. The app opens the back camera.
2. The user goes to the target.
3. The user takes a new live photo.
4. `ImageSimilarity.kt` compares it with the reference.
5. The alarm stops if the score reaches the configured threshold.

### Comparison method

This mission does **not** upload photos and does not use a cloud AI service. It performs a lightweight local perceptual comparison using:

- Relative grayscale brightness pattern
- Horizontal edge directions
- A small average-color contribution

The structural and edge parts make it less dependent on absolute brightness. This helps with normal day/night or room-light differences.

The comparison still needs the target to be visible. A completely black photo cannot be reliably matched. Best results come from:

- A fixed object or location
- A similar camera angle
- Enough light to see major shapes
- Avoiding moving targets such as people, vehicles, or changing screens

## Mission 2: Read aloud

The app selects one passage from a small built-in list and displays it as two sentences.

When the user taps the microphone:

1. The app requests microphone permission if needed.
2. Android `SpeechRecognizer` listens.
3. Android returns possible recognized text.
4. The app normalizes punctuation and capitalization.
5. It compares expected words with recognized words.
6. The mission succeeds when word coverage is high enough.

NoSnooze itself does not send recordings to its own server. Depending on the Android device and installed speech-recognition provider, speech processing may be on-device or may follow that provider’s network/privacy behavior.

## Mission 3: Awake selfie

1. CameraX opens the front camera.
2. The user takes a selfie.
3. ML Kit detects faces in the image.
4. The largest visible face is selected.
5. ML Kit provides left-eye and right-eye open probabilities.
6. Both values must pass the threshold.

The image is processed locally by the bundled face-detection library. The current check confirms a face with open eyes; it is not medical alertness detection and cannot guarantee that a person is mentally awake.

---

## 8. Complete alarm lifecycle

### Creating an alarm

```text
User enters alarm details
→ AlarmEditorScreen validates them
→ MainViewModel calls AlarmRepository
→ Room saves the record
→ AlarmScheduler schedules it with Android
→ Home screen updates automatically
```

### When the time arrives

```text
Android AlarmManager fires
→ AlarmReceiver loads the alarm
→ Repeating alarm schedules its next occurrence
→ Foreground ringing service starts
→ Sound loops and vibration starts
→ Full-screen notification opens AlarmActivity
→ Selected mission is displayed
```

### Completing the mission

```text
Mission reports success
→ AlarmActivity sends internal COMPLETE_ALARM action
→ Service stops MediaPlayer
→ Service cancels vibration
→ Foreground notification is removed
→ AlarmActivity closes
```

### After phone restart

```text
Android sends BOOT_COMPLETED
→ RescheduleReceiver reads enabled alarms from Room
→ Every enabled alarm is scheduled again
```

---

## 9. Local storage and privacy

NoSnooze does not require an account or its own server.

| Information | Storage location |
|---|---|
| Alarm times, labels, days, mission type | Room/SQLite database |
| Sound URI, volume, gradual mode, vibration | Preferences DataStore |
| Reference photos | Private app files directory |
| Temporary matching photos/selfies | Private app cache directory |

The app’s backup rules exclude its database and private files from cloud backup, reducing the chance of private alarm/photo information being copied to a cloud backup.

Uninstalling the app normally removes its private database, settings, reference photos, and cache.

---

## 10. Android permissions

| Permission | Why it is needed | When it matters |
|---|---|---|
| `CAMERA` | Capture reference, matching, and selfie photos | Photo/selfie missions |
| `RECORD_AUDIO` | Listen during read-aloud mission | Read-aloud mission |
| `POST_NOTIFICATIONS` | Show alarm notification | Android 13+ |
| `SCHEDULE_EXACT_ALARM` | Trigger alarms at exact times | Android 12+ |
| `VIBRATE` | Run vibration patterns | While ringing |
| `WAKE_LOCK` | Keep alarm processing/screen active | While ringing |
| `USE_FULL_SCREEN_INTENT` | Open mission over lock screen | Alarm notification |
| `FOREGROUND_SERVICE` | Keep alarm service active | While ringing |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Declare alarm audio service type | New Android versions |
| `RECEIVE_BOOT_COMPLETED` | Restore alarms after restart | Phone reboot |

Android 14+ may require the user to separately allow full-screen alerts. NoSnooze provides a shortcut in its settings page.

---

## 11. Sound and vibration

### Sound

The user chooses a sound through Android’s ringtone picker. DataStore saves its URI. If there is no custom sound, the app uses Android’s default alarm sound, then falls back to the default ringtone if needed.

`MediaPlayer` uses `USAGE_ALARM`, loops the audio, and applies the in-app playback level.

### Gradual volume

When enabled, playback starts at approximately 15% and increases in steps over about 18 seconds until it reaches the selected app level.

### Vibration

- **Off:** no app vibration
- **Steady:** long repeated pulses
- **Intense:** alternating short and long repeated pulses

System settings still have final authority. Do Not Disturb rules, system alarm volume, missing permissions, battery restrictions, or manufacturer-specific behavior can affect an alarm.

---

## 12. UI design

The UI is built completely with Jetpack Compose and Material 3.

The visual system uses:

- Warm cream background
- Dark “ink” cards
- Orange as the urgent action color
- Mint for photo missions
- Lavender for read-aloud missions
- Sky blue for selfie missions
- Rounded cards and clear large alarm times

`Theme.kt` stores the color scheme. Reusable Compose functions keep mission cards and headers consistent.

---

## 13. Performance choices

- Room and image comparison run away from the main UI thread using coroutines.
- The alarm list uses Flow, so only data-driven UI updates are needed.
- CameraX manages camera lifecycle automatically.
- Images are reduced to tiny grids during comparison instead of comparing every full-resolution pixel.
- The foreground service handles sound separately from the activity, so leaving the mission screen does not stop the alarm.
- Temporary mission images use cache storage and can be removed by Android when space is needed.

---

## 14. Building without Android Studio

The repository contains:

```text
.github/workflows/build-apk.yml
```

GitHub Actions performs these steps:

1. Checks out the source code.
2. Installs Java 17.
3. Sets up Android SDK 35.
4. Downloads Gradle 8.9.
5. Runs `:app:assembleDebug`.
6. Renames the output to `NoSnooze-v1.0.0-beta.apk`.
7. Uploads it as the `NoSnooze-APK` artifact.

A new build runs when source changes are pushed to the main branch. It can also be started manually from GitHub’s Actions page.

GitHub Actions artifacts currently expire after 30 days. A GitHub Release attachment is better for a long-lived public download.

---

## 15. Debug APK versus production release

The current beta APK is debug-signed.

### Debug APK

Good for:

- Personal testing
- Small beta groups
- Finding bugs
- Early MediaFire/GitHub sharing

Limitations:

- Not suitable for Google Play publication
- Future cloud builds may use another debug certificate
- An APK signed with a different certificate cannot update an installed copy; users may have to uninstall first

### Proper release APK/AAB

For long-term public use, create one permanent private signing keystore and protect it carefully. Every update must use:

- The same application ID: `com.nosnooze.alarm`
- The same signing key
- A higher `versionCode`

Never commit a release keystore or its passwords to GitHub.

For Google Play, an Android App Bundle (`.aab`) is normally preferred. For MediaFire or direct installation, a signed release APK is needed.

---

## 16. Versioning

Current Gradle values:

```text
versionCode = 1
versionName = 1.0.0
```

For the next update, use something like:

```text
versionCode = 2
versionName = 1.0.1
```

`versionCode` must always increase. `versionName` is the readable version shown to people.

---

## 17. Known limits

1. No app can prevent force-stop, uninstall, shutdown, or permission removal.
2. Exact timing requires Android’s exact-alarm access.
3. Full-screen behavior depends on permission and Android/manufacturer rules.
4. Speech recognition availability and network behavior depend on the phone’s speech provider.
5. Photo matching tolerates lighting changes but cannot identify an invisible target in darkness.
6. The selfie mission checks open-eye probability, not true mental alertness.
7. A user can leave the mission activity, but the foreground service continues ringing.
8. Do Not Disturb and system alarm-volume settings can influence playback.
9. Aggressive battery-management systems on some brands should be tested carefully.
10. The current beta uses debug signing and should be replaced with stable release signing before wide distribution.

---

## 18. How to test correctly

Use a physical Android phone and test all of these:

### Basic alarm

- Set an alarm two minutes ahead.
- Lock the phone.
- Confirm the screen turns on.
- Confirm notification, sound, and vibration.
- Confirm the ordinary back button does not dismiss it.

### Photo mission

- Save a reference target away from bed.
- Test from a similar angle.
- Test under brighter and darker lighting.
- Confirm an unrelated object does not pass.

### Read aloud

- Grant microphone permission.
- Read all words clearly.
- Try incomplete text and confirm it fails.
- Test with the device’s normal language/accent settings.

### Selfie

- Test in good and poor lighting.
- Test eyes open and eyes closed.
- Test when no face is visible.

### Reliability

- Restart the phone and confirm enabled alarms remain scheduled.
- Change the timezone or clock and inspect the next alarm.
- Disable and re-enable an alarm.
- Test one-time and repeating alarms.
- Test with the app removed from recent apps.
- Check behavior under Do Not Disturb and battery saver.

Do not rely on a beta alarm for an important appointment until it has passed repeated tests on that exact phone model.

---

## 19. Common changes and where to make them

| Desired change | Main file |
|---|---|
| Change app colors | `ui/theme/Theme.kt` |
| Change home screen | `ui/screens/HomeScreen.kt` |
| Change alarm form | `ui/screens/AlarmEditorScreen.kt` |
| Add another mission | `data/Alarm.kt`, editor, and `AlarmActivity.kt` |
| Change photo threshold | `ui/screens/PhotoMatchChallenge.kt` |
| Change photo algorithm | `util/ImageSimilarity.kt` |
| Change read passages | `ui/screens/ReadAloudChallenge.kt` |
| Change speech pass score | `ui/screens/ReadAloudChallenge.kt` |
| Change eye-open threshold | `ui/screens/SelfieChallenge.kt` |
| Change vibration patterns | `alarm/AlarmRingingService.kt` |
| Change scheduling rules | `alarm/AlarmScheduler.kt` |
| Add database fields | `data/Alarm.kt`, database version/migration, editor |
| Change permissions/components | `AndroidManifest.xml` |
| Change dependencies or SDK version | `app/build.gradle.kts` |
| Change cloud build | `.github/workflows/build-apk.yml` |

When changing Room entities after release, increase the database version and add a migration. Otherwise existing users may lose access to stored alarms or the app may fail to open the database.

---

## 20. Ideas for future versions

Possible improvements include:

- Permanent release signing and automatic release builds
- Proper Room migrations
- Multiple reference photos per photo mission
- Better local feature matching for larger viewpoint changes
- Optional QR/barcode mission
- Step-count or walking mission
- Math mission
- NFC-tag mission
- Per-alarm sound and vibration instead of only global settings
- Alarm preview/test button
- Permission-readiness dashboard
- Battery-optimization guidance for different phone brands
- Accessibility testing and larger text modes
- Instrumented alarm reliability tests
- Localization into Urdu and other languages

---

## 21. Small glossary

**APK:** Installable Android application file.  
**AAB:** Android App Bundle used mainly for Google Play publishing.  
**API:** A system interface that lets code use Android features.  
**Activity:** An Android screen.  
**BroadcastReceiver:** Code that reacts to Android or alarm events.  
**Compose:** Android’s modern Kotlin UI toolkit.  
**Coroutine:** A safe way to perform background/asynchronous work.  
**DataStore:** Modern local key-value settings storage.  
**Foreground service:** Long-running work with a visible notification.  
**Flow:** A stream of values that updates observers automatically.  
**Gradle:** Android build and dependency system.  
**KSP:** Kotlin code-generation system used here by Room.  
**Manifest:** File declaring permissions and Android components.  
**PendingIntent:** Permission for Android to perform a future app action.  
**Room:** Kotlin-friendly layer over SQLite.  
**SDK:** Tools and Android APIs used to build an application.  
**SQLite:** Local relational database inside Android.  
**URI:** A stored reference to a selected sound or resource.  
**ViewModel:** Holds UI-related logic and data outside individual screen redraws.  

---

## 22. Final summary

NoSnooze combines a modern Compose interface with Android’s native alarm infrastructure. Room keeps alarm records, DataStore keeps user preferences, AlarmManager schedules precise events, a foreground service keeps sound and vibration alive, and three local/device-assisted missions make waking up active instead of passive.

The current release is a functional beta and successfully builds through GitHub Actions. Before treating it as a production product, the most important next steps are extensive physical-device testing and permanent release signing.
