# NoNo Companions

A private native Kotlin Android app for your two chibi companions, using the approved second concept sheet. Version 0.5 adds keyboard retreat/peeking and deliberate smooth quarter-turn orientation, retaining automatic parachutes and smaller companions, French dialogue, gravity/tilt reactions, generated speech and action-menu skins, and 44 pose/animation frames per spouse. Long-press a character for actions, including a coordinated kiss with floating hearts. See [the latest update](docs/VERSION-0.5.md).

## Install

1. Download the APK from the [private GitHub release](https://github.com/kb777only/nono-companions/releases/latest) while signed into your GitHub account, then open it on each phone and allow installation from that source when Android asks. This is a debug-signed APK, suitable for private testing.
2. Open **NoNo Companions**, choose **Allow companions over apps**, and grant Android's overlay permission.
3. Return to the app and choose **Welcome our companions** once. Allow the service notification if desired; Android still provides its active-app controls.
4. Optional: choose **Optional app & keyboard awareness**, read the explanation, and enable NoNo in Android Accessibility settings. This provides keyboard-window detection across apps; without it, avoidance depends on vendor overlay insets. After updating, re-enable this service if keyboard detection does not respond. Sideloaded-app restricted settings may require an additional explicit approval in Android App info before Accessibility can be enabled.
5. Tap for a reaction. Hold still for about half a second to open the character’s menu; move beyond Android’s touch threshold to drag instead. Choose **Bisou** for a kiss when both are settled on the floor. Shared sequences begin autonomously after a quiet interval. There is no pause button, floating toolbar or routine hide control. Use Android's permission/service controls when needed.

If HyperOS or Samsung stops the app, review the app's background/battery settings or reopen it. Android may suppress overlays on secure screens and stop the process; uninterrupted presence is not guaranteed. Do not disable platform safeguards to force the app onto protected screens.

## Implemented

- Two independently positioned overlay companions; separate, non-touchable bubbles and temporary props.
- Wandering, blinking, resting, tap reactions, gravity-driven falls/landings after drag, food and personal antics. Phone tilt changes the falling direction.
- One coordinated controller for snack theft in either direction, notice/chase/sharing, dinosaur call-and-response (`dinsoauurr...` then `rawrrr`), and affection.
- Ownership transfers animate the independent snack prop between its owners; interrupted scenes clean up their objects and participants.
- Mood-weighted choices, quiet intervals, cooldowns and warm local relationship progression.
- Atomic local mood persistence, gentle bounded offline changes, screen-off suspension, supported service restart attempts and boot restoration.
- Optional coarse foreground-app categories and keyboard window bounds. No screen text or accessibility tree reading, other-app operation, raw content history, backend, accounts or network permission.
- Bounds updates for delivered display/configuration/inset changes, with interaction cancellation and position clamping.

## Current limits

- 44 generated pose/animation frames per spouse with timed clips. Some walk phases are subtle, exact hand contacts are approximate, and there is some generated pose-to-pose detail drift. The approved concept remains the source of truth. This is not a claim of studio-quality animation.
- Overlay touch targets are small rectangles, not exact alpha silhouettes. Transparent space within each rectangle also receives touches.
- Fold-hinge avoidance is not implemented. Galaxy Z Fold hardware, split-screen and vendor keyboard-inset behavior need device verification; some devices do not deliver the keyboard insets to other-app overlays.
- Accessibility categories can be unavailable or unspecified; autonomous behavior continues. No attempt is made to infer sensitive content.
- The current implementation uses the primary display. No guarantee of recovery after user force-stop or aggressive vendor battery termination.

## Build

Open this directory in Android Studio, or install JDK 17/21 and Android SDK platform 35 and run:

```powershell
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Set `sdk.dir` in your local `local.properties` if needed. The wrapper pins Gradle 8.11.1, Android Gradle Plugin 8.7.3 and Kotlin 1.9.24. Build output: `app/build/outputs/apk/debug/app-debug.apk`. No paid service/API key is needed. This environment uses its already-installed SDK and cached dependencies.

See [architecture and Android requirements](docs/ARCHITECTURE.md), [verification record](docs/VERIFICATION.md), and [current art manifest](art/v2/manifest.json). The transparent art pack is also available in `delivery/NoNo-Art-v5.zip`.
