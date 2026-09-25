# Orbit Drive — Android prototype

Native Kotlin/Jetpack Compose companion to the iPhone prototype. Gameplay includes launch timing, flight and bounce simulation, six research paths, five clubs, planet HP and persistent damage, and ascension. Saves remain on device.

## Run

Open this folder in Android Studio, sync the Gradle project, and run `app` on an Android 8.0 (API 26) or newer emulator or device. The project uses Android Gradle Plugin 8.7.3, Kotlin 2.0.21, JDK 17, compile SDK 35 and Gradle 8.9. If Android Studio asks for a Gradle distribution, select Gradle 8.9. A wrapper is not bundled because this environment has no Gradle installation or Android SDK to generate and verify it.

Change `applicationId` before Play Store release. The current project is a playable first prototype, not a signed release bundle. Production work remains: device playtesting, polish, original artwork/audio, accessibility, balance, store listing, privacy/support information and signing. It has no ads or purchases yet. Android and iPhone saves are separate; there is no account sync.

## Build on GitHub using only a phone

This project includes `.github/workflows/android-apk.yml`. Put the **contents** of this folder at the root of a GitHub repository (keep the hidden `.github` folder). In GitHub, open Actions → Build Android test APK → Run workflow. When green, open the run → Artifacts → OrbitDrive-Android-test-APK. Download the artifact ZIP to your Android phone, extract `app-debug.apk`, tap it, and allow installation from your browser or file manager if prompted. This is a debug APK for personal testing. It is not suitable for Google Play publication.

The first cloud build is the actual compile check. If it fails, keep the workflow log; a source-only review cannot establish the APK works.
