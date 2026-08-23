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

Step 3 is the app's opening screen and there is no way past it: everything else
is switches that do nothing without the service. It notices the switch being
thrown and lets you through by itself, and it carries the unlock from step 2 as
a footnote for when the switch will not move. Tap **PREVIEW** any time to see
the screen without waiting to be caught.

## Getting in

Sit through the hold, tap **ENTER**, and you are let through for that visit.
Leave the app and the next arrival is stopped again. There is no clock to run
down and no allowance to spend: if an app is ticked, reaching for it is
interrupted, every time.

**ENTER** is the only way through. Backing out, letting the screen time out, or
pressing home all leave you where you started.

## What it does with your data

Nothing leaves the phone. There is no network code in this app at all.

The accessibility service is declared with `canRetrieveWindowContent="false"`,
so it cannot read what is on your screen. It receives window-change events and
looks at one field, the package name of the app that just came to the front. If
that package is on your list it launches the interstitial. That is the whole
mechanism.

That one line is also what makes the app installable by hand. Turn it on and
Android stops offering a plain switch and asks instead whether this app may
"have full control of your device", because a service that can read the screen
can read everything on it, and Play Protect starts warning about the developer
at install time. v0.2 and v0.4 turned it on so they could tell Reels and Stories
apart from the rest of Instagram, and that is what they cost. Watching one screen
inside an app cannot be done without it, so this app does not do it.

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
| `RattAccessibilityService.kt` | Reads foreground package names, fires the interstitial |
| `Watchlist.kt` | Which apps are watched, stored in prefs |
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

An app is one thing to it, so ticking Instagram catches the messages along with
the feed. Telling Reels apart from the rest of Instagram means asking for the
screen-reading capability, which costs the plain install and the plain switch,
so it is all or nothing per app.

And the usual caveat for this whole category: novelty wears off in about two
weeks. Interrupting the reflex is the easy half.
