# Dopamine Ratt

Open a watched app and this gets there first: a neon interstitial, a haptic knock,
and four seconds you have to sit through before the way in appears.

An Android take on the one-sec idea. Pick the apps that eat your day, and every
time you reach for one you get interrupted by a glowing sign instead.

## Install

Grab the APK from [Releases](../../releases/latest) and open it on your phone.

Three steps, and the third one is the part everyone gets stuck on:

1. **Install the APK.** Your browser or file manager will ask for permission to
   install unknown apps. Allow it. Play Protect may warn that the developer is
   unrecognised; tap through.

2. **Allow restricted settings.** Android 13 and up blocks sideloaded apps from
   using accessibility until you explicitly unlock it. Go to
   **Settings → Apps → Dopamine Ratt → ⋮ (top right) → Allow restricted settings**.
   Skip this and the next step is greyed out and the app looks broken.

3. **Turn on the service.** Open the app, tap **TURN IT ON**, and enable
   Dopamine Ratt in the accessibility list. Then **CHOOSE APPS** and tick what
   you want interrupted.

Tap **PREVIEW** any time to see the screen without waiting to be caught.

## What it does with your data

Nothing leaves the phone. There is no network code in this app at all.

The accessibility service is declared with `canRetrieveWindowContent="false"`,
so it cannot read what is on your screen. It receives window-change events and
looks at one field, the package name of the app that just came to the front. If
that package is on your list it launches the interstitial. That is the whole
mechanism.

Your watchlist lives in `SharedPreferences` on the device.

## Using your own artwork

The neon emblem is drawn in code, but the app will light up an image instead if
it finds one. Drop a file into the app's own directory:

```sh
adb push your-image.png /sdcard/Android/data/com.dopamine.ratt/files/emblem.png
```

`emblem.png`, `.jpg`, `.jpeg` and `.webp` all work. It gets the same bloom,
chromatic ghosting and flicker as the built-in mark. Delete the file and it
falls back. Nothing is baked into the APK, so whatever you point it at stays
yours and stays on your device.

## Build it yourself

```sh
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

For a signed release build, create `keystore.properties` in the project root:

```properties
storeFile=your-release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Then `./gradlew :app:assembleRelease`. Without that file the release build is
simply unsigned. The keystore and its properties are gitignored on purpose.

## How it is put together

| File | What it does |
|---|---|
| `RattAccessibilityService.kt` | Reads foreground package names, fires the interstitial |
| `Watchlist.kt` | Which apps are watched, stored in prefs |
| `Gate.kt` | The 20 second pass after you choose to go in |
| `InterstitialActivity.kt` | The screen, the hold, the two ways out |
| `NeonSign.kt` | Bloom, chromatic fringe, breathing, flicker |
| `Haptics.kt` | The knock on arrival |
| `SetupActivity.kt` | Status, app picker, preview |
| `Apps.kt` | Reading the installed app list |

Built with Compose. Gradle 8.14.3, AGP 8.13.2, Kotlin 2.2.21, compileSdk 36,
minSdk 26.

Display type is [Bebas Neue](https://github.com/dharmatype/Bebas-Neue), SIL OFL.

## Honest limits

Once you are inside an app this has no idea what you are doing and no way to
reach you. It interrupts the reach, not the scrolling.

It also only sees entries through the normal app-switch path. Notification taps
and share sheets can land you somewhere without a window change it recognises.

And the usual caveat for this whole category: novelty wears off in about two
weeks. Interrupting the reflex is the easy half.
