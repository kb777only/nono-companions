# Version 0.5.1 verification — September 14, 2026

- Debug build, 42 tests and lint passed; no test failures.
- Five new tests cover nearest edge for either character, both shared sides, stable episode choice, narrow-height stacking bounds, and center-distance tie breaking.
- APK SHA-256: A81A026EF33BF7D6778AEA8EC6D1D3A3A9902829FFC8C70B857A7BC979DDDB77
- Existing generated peek sprites are mirrored for the chosen edge; no new artwork was needed.
- Phone disconnected: no device test or installation.

---

# Version 0.5 verification — September 14, 2026

- Final debug build, all **37 unit tests**, and Android lint passed. Lint: zero errors, six existing programmatic-view/absolute-gravity warnings.
- APK SHA-256: `F1528111B2EE3857E6EDAA6C1F663D1C1FBA6F447E250DF91A1A1DE94E654131`.
- Eight new tests verify tilt jitter rejection, 250 ms dwell, smooth quarter/half-turns, shortest-path wraparound, flat-device behavior, rotated footprints, keyboard scene interruption and quiet waiting, return movement, rapid keyboard/configuration changes, and metadata/inset fallback.
- Generated open-eye/blinking peek artwork inspected and real alpha preparation completed. The runtime atlas and original generated source are included.
- Accessibility implementation reviewed: only window metadata and app category are used; no root/node/text retrieval calls. Own overlay moves do not trigger repeated keyboard enumeration. Inset-only detection remains vendor dependent; the optional window-metadata capability is explained in onboarding.
- **No device test or installation:** phone remains disconnected. Cross-app IME visibility on HyperOS/Samsung, actual rotation feel, peek placement, accessibility re-enable behavior and Fold layouts remain hardware checks.
- Main and the release are published to the existing private repository. Release assets include the APK and full runtime art pack.

---

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




## 0.6.0 — 2026-09-14

Offline debug build, 59 unit tests (zero failures/errors), and lint passed (zero errors, seven warnings). Final APK contains byte-identical cooling, cooling-props, cold/hot/night wardrobe and umbrella PNGs. Android APK signing verification passed. No INTERNET permission in packaged manifest. New processed atlases contain 38 nonempty cells with real alpha. No device/emulator test was performed because the phone is disconnected and no emulator is configured. Live weather remains unconnected pending Open-Meteo coordinate-sharing approval.

APK SHA-256: `0C164B23E79F9CCA763F54C757B27C97F9A293F1B9A6D80ED3D80C22E9CDBD23`. See VERSION-0.6.md for behavior, art, privacy and limitations.


## 0.7.0 — 2026-09-14

Debug build passed; 68 tests passed with zero failures/errors. Lint: zero errors, 20 warnings (13 localization warnings in the French developer UI, plus seven existing custom-view/RTL warnings). APK signing verified. No device/emulator UI test: phone disconnected. New tests cover independent overrides, persistence decoding, current Auto inputs, precise French decimal input, invalid values, weather/temperature independence, heat priority, orientation easing/gravity and keyboard/context mappings.

APK SHA-256: `dbcef45ad2f9f9181f448fbc18711d5d8cd3bc02ef578fc2bae4b207b95f8bd2`.
