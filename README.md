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

Steps 2 and 3 are the app's opening screen, one at a time, and there is no way
past them: everything else is switches that do nothing without the service. The
service step notices the switch being thrown and moves on by itself. Tap
**PREVIEW** any time to see the screen without waiting to be caught.

## Getting in

Sit through the hold, tap **ENTER**, and you are let through for that visit.
Leave the app and the next arrival is stopped again. There is no clock to run
down and no allowance to spend: if an app is ticked, reaching for it is
interrupted, every time.

**ENTER** is the only way through. Backing out, letting the screen time out, or
pressing home all leave you where you started.

## Reels and Stories

Instagram can be stopped at Reels or at Stories without stopping the rest of it.
Both are off until you turn them on, in the picker, under Instagram.

They work whether or not Instagram itself is ticked. Untick Instagram and leave
Reels on, and messages and posts are left alone while the feed of clips is not.
An app with a surface armed stays at the top of the picker so you can find the
switch again.

## What it does with your data

Nothing leaves the phone. There is no network code in this app at all.

For the watchlist, the service receives window-change events and looks at one
field, the package name of the app that just came to the front. If that package
is on your list it launches the interstitial.

Reels and Stories need more than that, and it is worth being plain about it. The
service is declared with `canRetrieveWindowContent="true"`, because a capability
cannot be asked for later. What it does with it is narrow:

- The events and flags that let it look are only asked for while Reels or
  Stories is armed, and handed back when you switch the last one off. Until
  then the service sees package names and nothing else.
- It reads `viewIdResourceName`, the developer's name for a container, and never
  the text inside one. It is looking for `clips_viewer` and `reel_viewer`, which
  are Instagram's internal names for those two screens.
- The walk stops after 400 nodes, runs at most twice a second, ignores anything
  off screen, and only runs at all in an app with a surface armed.

Your watchlist lives in `SharedPreferences` on the device.

## Using your own artwork

The app ships with its own rat sign, but it will light up an image of yours
instead if it finds one. Drop a file into the app's own directory:

```sh
adb push your-image.png /sdcard/Android/data/com.dopamine.ratt/files/emblem.png
```

`emblem.png`, `.jpg`, `.jpeg` and `.webp` all work. It gets the same wash,
breathing, pulse rings and glitch as the built-in sign. Delete the file and the
built-in one comes back. Whatever you point it at never leaves your device.

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
| `RattAccessibilityService.kt` | Reads foreground package names, spots armed surfaces, fires the interstitial |
| `Watchlist.kt` | Which apps and surfaces are watched, stored in prefs |
| `Surfaces.kt` | Reels and Stories, and how they are recognised |
| `Gate.kt` | The one visit that ENTER buys, and nothing more |
| `InterstitialActivity.kt` | The screen, the hold, the two ways out |
| `NeonSign.kt` | The wash, the sign, breathing, pulse rings, glitch, embers |
| `Haptics.kt` | The knock on arrival |
| `Onboarding.kt` | The gate in front of the app until the service is on |
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

Reels and Stories are recognised by names Instagram chose for its own use and
can rename whenever it likes. If a release moves them, that stops working until
the markers in `Surfaces.kt` are updated. The rest of the app does not depend on
it.

And the usual caveat for this whole category: novelty wears off in about two
weeks. Interrupting the reflex is the easy half.
