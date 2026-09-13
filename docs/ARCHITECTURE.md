# NoNo implementation notes

This page records the initial 0.1 implementation. [Version 0.2](VERSION-0.2.md) supersedes its artwork, sizing, physics, dialogue and animation scheduling sections; the command, persistence and Android permission boundaries remain applicable.

The workspace was empty; there were no project instructions or existing code to reuse. The approved second concept was recovered from this task's generated-image directory and copied to `art/approved-concept.png`. Kotlin with platform Android Views avoids unnecessary framework dependencies. This private prototype targets API 35, supports API 31+, and does not include internet permission, accounts, pairing, Supabase, Vercel, or a remote transport.

## Simulation and coordination

`World` is a pure Kotlin simulation driven by monotonic milliseconds and injectable randomness. `Command` is the single input boundary, with stable HUSBAND/WIFE identities. Future transport should validate/rate-limit incoming commands and map only allowed actions to this boundary; dragging and screen context remain local. No remote entry point is implemented.

Priority is explicit: drag/geometry interruption > tap reaction > an active coordinated interaction > contextual observing > weighted autonomous choices. An interaction owns both participants, its stage timer, leader/partner roles and prop. Snack stages are produce → approach → transfer ownership → notice → chase → negotiate → share → cleanup. Either spouse can own the snack; the other is the thief. Missing/expired props, geometry changes, drag, or a 30-second safety deadline cancel the interaction, clear bubbles/props and recover both participants. Kind-specific cooldowns apply on finish and interruption. Quiet choices alternate with action; only one shared sequence and one speech bubble can exist.

Needs are bounded and affect choices: hunger and partner mischief weight snacks; affection and bond weight affectionate interactions; low energy chooses rest. Rest restores energy. Completed shared interactions raise bond gently. `MoodCodec` stores only needs, bond, version and wall time. AtomicFile handles replacement. Offline elapsed time is capped at eight hours, adds mild hunger and restores energy; affection/bond never decay. Monotonic tick deltas are capped. State saves every minute, on drag release/tap, screen-off, and service teardown. Transient interactions restart cleanly rather than resuming a stolen prop halfway through a scene.

## Android boundary

`CompanionService` starts from visible onboarding and uses the `specialUse` foreground-service type with its manifest explanation and a low-importance notification. It uses START_STICKY; boot restoration is attempted only after setup and while overlay permission remains granted. Android/vendor process management and user force-stop can prevent restart. There are no wake locks, alarms, retry loops, or battery-optimization bypasses. Reopening the launcher starts the service again.

Each companion occupies its own small TYPE_APPLICATION_OVERLAY window (at most 108 × 152 dp), with NOT_FOCUSABLE/NOT_TOUCH_MODAL. There is no full-screen overlay canvas. The rectangular touch footprint includes small transparent margins; this is not a pixel-perfect silhouette touch region. Props use a separate 28 dp non-touchable overlay and interpolate ownership transfers over 450 ms; interruption removes that surface. The single bubble is a separate NOT_TOUCHABLE window at 0.75 opacity, placed above/below its speaker and clamped. Android's restrictions still apply to protected windows; overlays are not accessibility overlays. Screen-off removes ticks and bubbles; the lock screen hides characters. Idle checks are 1 Hz with no invalidation if the frame/prop/facing is unchanged, active simulation at 12.5 Hz. Drag movement uses input events. Accessibility only consumes debounced window-state package identity and maps ApplicationInfo category to a small signal; no tree/text access, screenshots, gesture execution, package history or screen logging.

WindowMetrics, system bar/cutout insets, display/configuration listeners and delivered IME insets drive bounds. Resizes cancel coordinated activity and clamp both pets. This is one primary-display overlay, not an app window attached to a split-screen pane. Fold hinges/occlusion are not explicitly mapped. Some vendors do not deliver other apps' IME insets to overlay windows; keyboard avoidance on those devices remains a known limitation. No display-size polling or accessibility tree scanning is used.

## Artwork and playback

`Art` loads two 4 × 2 pose atlases once. Each row represents one spouse, with full-body, bottom-center anchors; alpha bounds remove cell margins. Sheet A: idle, step, eating, dinosaur claws. Sheet B: rest, startled, open arms, personal crouch/squat. Wife renders at 90% of nominal cell scale; husband at 97%. Props are separate Canvas shapes in their own small surface with ownership, animated transfer and expiry.

The built-in image generator created the source atlases from the approved concept but repeatedly returned RGB with painted checkerboards. `tools/prepare-art.ps1` performs reproducible border-connected background extraction into real RGBA PNGs and retains source art. Inspect edges, especially glasses/hair, when replacing assets. This step is not a claim of animation polish.

`AnimationPlayer` is separate from decisions: state changes reset entry time, clips loop a few pose frames or hold a non-looping final frame, and interruptions switch immediately. This is limited pose-based animation, not finished hand-drawn animation. Missing: opposite-stride walk frames, reach/hand-off/chase transitions, precise hand attachment metadata, blink/breathing frames, hug contact/in-betweens, proper repair poses, and more distinct wife's expressions. Some generated poses lose the hoodie emblem or vary details. The visual system is usable for evaluating behavior, not final approved production art.

## Official requirements checked September 13, 2026

- [Foreground service types, including specialUse](https://developer.android.com/develop/background-work/services/fgs/service-types): type-specific declaration/permission and subtype explanation.
- [Android 15 changes](https://developer.android.com/about/versions/15/behavior-changes-15): an overlay permission alone is insufficient for the overlay-based background-start exemption; boot is a separate permitted exemption for applicable types. Normal initial start occurs from the foreground activity.
- [Overlay window flags and types](https://developer.android.com/reference/android/view/WindowManager.LayoutParams): overlays stay below critical system windows; small separate windows avoid covering the display with a touch target.
- [Android 12 untrusted touches](https://developer.android.com/about/versions/12/behavior-changes-all#untrusted-touch-events): non-touchable application overlays have obscuring-opacity constraints.
- [AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService): explicitly user-enabled service; content retrieval and gesture execution are disabled in this implementation.

This is a private sideload build, not a Play Store policy approval or a promise of indefinite background operation.

