The companions now get out of the way while you type: they retreat to opposite screen edges, peek and blink using newly generated artwork, then return when the keyboard closes. Their windows are pass-through while typing, and shared antics, speech and props wait quietly.

They also rotate smoothly to **0°, 90°, 180° and -90°**. A 55° threshold and 250 ms settling delay prevent jitter; quarter turns take 450 ms and half turns 650 ms. Gyro-fused game rotation vector data is preferred, with gravity/accelerometer fallbacks. Props and parachutes follow the character rotation.

### Install and keyboard setup

Download **NoNo-Companions-debug.apk** below, open it and accept the update. Open NoNo and enable **Optional app & keyboard awareness**. If already enabled, turning it off and on may be necessary after this update. Android requires window-content capability to expose keyboard-window metadata, but NoNo only inspects window types/IDs/bounds and foreground app categories: it never requests screen text, passwords, fields or accessibility nodes, and never operates other apps.

Without this optional access, keyboard avoidance relies on whatever IME insets your phone exposes to overlays and may not work across every app. Floating keyboards conservatively reserve the area below their top; if almost no room remains, the companions temporarily stay fully out of view until typing ends.

### Checks

Debug build, **37 unit tests**, and Android lint passed (zero errors, six non-blocking warnings). The phone is disconnected, so this update has **not been device-tested or installed**. Actual keyboard behavior, rotation smoothness and Fold layouts still need hardware verification.

The artwork ZIP includes all runtime assets and the updated manifest: 44 pose/animation frames per spouse. See docs/VERSION-0.5.md for implementation details, the image-generation prompt and official Android references.

APK SHA-256: `F1528111B2EE3857E6EDAA6C1F663D1C1FBA6F447E250DF91A1A1DE94E654131`
