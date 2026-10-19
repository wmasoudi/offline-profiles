# Offline Profiles

An offline Android sound profile manager with manual profile switching, temporary profile overrides, and weekly time rules. It is written in Java and targets Android 16 (API 36).

## Features

- Profiles for ringer mode and ringtone, notification, media, alarm, and system volumes.
- Manual and timed profile application, with quick durations, a custom duration, or an end time.
- Automatic restoration of the sound settings captured before a temporary switch.
- Weekly schedules with selectable weekdays and local times.
- Separate Profiles and Schedules tabs.
- Local-only preferences, no ads, no `INTERNET` permission, and app data excluded from cloud backup and device transfer.

## Install

Build the APK as described below, copy `dist/OfflineProfiles-1.1.0-android16.apk` to an Android device, and open it to install. Android may ask you to allow APK installation from the app that opened the file.

Grant **Do Not Disturb access** for Silent/Normal sound mode changes. Grant **Alarms & reminders** for precise schedule and timed-return behavior; without it, Android may delay alarms. Verify the sound streams on your device because Android and device manufacturers may couple or restrict some volume settings.

The app shows the last profile it applied. It cannot detect profile changes made outside the app. A temporary application snapshots the current sound settings and restores them at the selected end time. Weekly schedules are held during that temporary period. If the app is force-stopped, open it again to resume its alarms.

## Build

The project uses Android API 36, JDK 17, and Android build tools. `build.py` expects these local tools under `.toolchain/`:

- `jdk-17.0.20.1+1/`
- `android-37.0/`
- `android.jar`

Run `python build.py` from the project root. The script compiles the Java sources, packages and aligns the APK, signs it, and verifies the signature. The keystore and password are local build files under `.toolchain/`; they are intentionally not included in this repository. Preserve your own keystore if you need to sign future updates with the same certificate.

The APK is not checked into this repository. Builds have been verified for packaging and signature, but should be tested on a physical device before relying on scheduled sound changes.
