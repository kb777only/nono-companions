# Version 0.4 verification — September 14, 2026

- Debug APK build, all **29 unit tests**, and Android lint passed. Lint: zero errors, six warnings (five programmatic custom-view constructors and intentional absolute overlay gravity).
- APK SHA-256: `DAD98C272A1381D4B8968198B602C9B045D70F3181EAD7CE6E9A5D31ABA38FE4`.
- Five new tests cover the exact 650 ms deployment boundary, reduced glide speed, drag/release timer reset, independent spouse timers, landing/display/screen-off cleanup, and gravity-axis speed limiting. The existing drag interruption test now waits for the intentionally slower physical landing.
- Generated parachute/pose atlas was inspected and its fallback magenta background removed; source and final alpha PNG are included. Runtime canopy/hand alignment has not been checked on a phone.
- **No device installation or test for version 0.4:** the user’s phone is disconnected. Existing Fold, insets, battery and precise art-alignment limitations remain.
- GitHub repository visibility was verified PRIVATE before source upload. Local SDK paths, build caches, signing material, device screenshots and release binaries are excluded from Git; the APK and art bundle are attached as release assets.

---

# Verification record — September 13, 2026

## Current build: version 0.3

- Debug build, 24 unit tests and Android lint passed. Zero test failures; lint reports zero errors and five non-blocking warnings (four programmatically constructed Views and deliberate absolute overlay gravity). The legacy soft-input resize constant remains deprecated; explicit inset handling is also present.
- Final APK SHA-256: `3B5AE92ED5E7DA8024C54D50CE00FCA3B035170DB71363160BD36BADF64ECE78`.
- Final version 0.3 APK installed successfully on connected Xiaomi 25010PN30G / Android API 36; launch requested successfully.
- New tests cover stationary-hold timing, tap suppression, drag priority before/after hold, gesture reset, kiss stages for either initiator, randomized per-face heart positions, all lifetimes bounded to 400–900 ms, particle expiry, interruption cleanup, airborne action rejection, and explicit rest. Existing tests cover moods, coordinated scenes, gravity, bounds and interruptions.
- Newly generated menu and kissing source art visually inspected; transparent atlases packaged with existing art. No animation is claimed to be studio-quality or frame-perfect.
- Version 0.2 device smoke checks verified smaller companions, French generated speech bubbles, and the falling pose after an upward drag. Android reported the app’s non-wakeup gravity sensor at 10 Hz and no wake lock. Screenshots: device-v2.png, device-v2-french.png, device-v2-falling.png.
- The phone went to sleep before version 0.3 menu/kiss visual checks. A screenshot attempt was black and power state reported Dozing; this does **not** verify the menu or heart rendering. The user was asked to unlock it. Unit tests are not a substitute for this pending visual check.

## Remaining hardware checks

Menu placement/labels and exact mouth/heart alignment on device; long-press versus drag feel; Fold folded/unfolded layouts and hinge occlusion; split-screen and vendor keyboard insets; lock/unlock, revocation, reboot and low-memory recovery; battery endurance. No Fold hardware or configured emulator was available. Fold-hinge avoidance is not implemented. Generated stride/contact transitions can still benefit from artistic refinement.

## Historical version 0.1 record


## Completed local checks

- `:app:assembleDebug`: successful, native debug APK produced.
- `:app:testDebugUnitTest`: 11 tests passed, zero skipped/failures/errors.
- `:app:lintDebug`: successful, zero errors, four warnings (two programmatically constructed custom Views, intentionally English bubble text, and deliberate absolute overlay gravity). Compiler also reports the legacy soft-input resize constant as deprecated; explicit inset listeners are used as well.
- Inspected both generated atlases visually and checked real alpha extraction. Sheet A contains 942,454 transparent pixels; sheet B contains 894,613 of 1,572,864 pixels each. Source atlases and the approved concept are preserved.
- Packaged `delivery/NoNo-Companions-debug.apk`, SHA-256 `923A5C416666D74CD0A9B70F06CAE0CCEFC7126CBDF771C5FB95C55B1CB4A361`.

Tests cover snack transfer/reconciliation with either owner; every interaction's termination; drag interruption of either spouse at different stages; missing props; geometry cancellation/clamping; exact dinosaur conversation turns and cooldown; mood serialization and bounded elapsed time including invalid values; low-energy resting; context rate limits; animation interruption; and drag cleanup on display/screen interruption.

## Device setup

Connected Xiaomi reports model `25010PN30G`, Android API 36. No configured Android emulator or Fold device was available. The first installation was rejected by the device; after the user requested another prompt, installation succeeded. App launch and onboarding layout were visually inspected, and the overlay permission was observed as granted. The user completed optional Accessibility setup; both services were observed running. Android reports the companion service as foreground, specialUse type `0x40000000`.

Both transparent companions were visually verified on the device. A drag moved the wife to a new location; a tap showed the startled pose and the bubble “Her: Come closer.” A tap outside their small windows successfully opened the underlying app's dialog. The app's private mood file was read to verify that needs and a progressed bond value had been written. No app crash appeared in the inspected crash buffer. Device inspection found an extra overlay vertical offset and reversed wife-facing direction, both corrected in the delivered build. Screenshots are retained alongside this record. This is a smoke test, not a full hardware acceptance suite.

## Still requiring verification

Galaxy Z Fold folded/unfolded geometry and hinge occlusion, split-screen, keyboard avoidance across vendor apps, screen-off/relock restoration, permission revocation, boot and low-memory process restart, battery endurance, and long-running interaction variety. Unit tests of geometry/interruptions do not substitute for these hardware checks.

The artwork is a prototype set of distinct poses and limited loops, not a complete polished animation set. Background extraction has visible small residual islands around some glasses/hair gaps. Precise hand contacts, stride cycles and in-between poses remain missing.

