# Version 0.2 — smaller, French, gravity and animation artwork

This update follows the user's on-device feedback. Both companion windows are now 72 × 104 dp (previously 108 × 152 dp). Speech uses generated ivory/sage and lavender/plum manga skins with native French text, rather than the original plain rounded rectangles. Notification copy is French as well. The private dinosaur joke remains exactly `dinsoauurr...` followed by `rawrrr`.

## Physics

`GravityPhysics` applies acceleration and velocity with substeps no larger than 1/60 second. Characters are pinned only while actively dragged; release restores gravity and bounded release velocity. Edge impacts have a small restitution and a short landing animation. Settled characters have friction. At rest the pair gently separates so neither completely hides the other.

`TiltSensor` uses Android's fused TYPE_GRAVITY sensor, normally backed by accelerometer/gyroscope fusion. It falls back to a low-pass accelerometer, remaps natural-device axes to the actual display rotation, and changes the direction of falling as the phone turns. It does not integrate raw gyro rates, which would drift. When the phone lies flat and screen-plane gravity is nearly zero, the intentional fallback is ordinary screen-down gravity. If there are no suitable sensors, default downward gravity still works. There is no sensor permission prompt or high-rate sensor permission. Sensor delivery is requested at 10 Hz, and registration stops with the screen off/locked.

Falling uses 32 ms simulation ticks; active choreography uses 65 ms. Quiet characters schedule the next animation-frame boundary, capped at one second for behavior/needs, and are not redrawn if their frame and facing are unchanged. Movement changes the walk direction; quiet poses look toward the partner. Geometry changes and loss of ground during a shared scene cancel that scene and its props. Independent activity resumes after recovery and cooldown.

## Artwork and clips

The built-in image generator produced six 12-cell character atlases: motion, social and personal for each spouse, giving 36 source frames each. It also generated two bubble skins and six separate prop images (whole/bitten cookie, snack packet, wrench, plum heart plush and dinosaur plush). Characters remain human. Cookie, wrench and heart are used now; the packet and dinosaur are available as optional assets.

Source art is preserved in `art/v2`. `tools/prepare-art-v2.ps1` removes the explicitly requested magenta matte, cleans generator-added grid rules, preserves RGBA, and scales atlases for phone memory usage. Generated art still has small pose-to-pose differences; this is a frame-animated private app, not a claim of studio-quality hand-authored animation. Some walk frames have subtle rather than large stride differences.

Frame map for each character:

| Frames | Action |
| --- | --- |
| 0–3 | Walk cycle |
| 4–7 | Idle and blink |
| 8–9 | Falling |
| 10–11 | Landing and recovery |
| 12–14 | Dinosaur claws |
| 15 | Mischievous smile |
| 16–18 | Approach/open-arm affection |
| 19 | Wink |
| 20–22 | Reach, grasp, retract |
| 23 | Mock outrage |
| 24–27 | Raise hand, bite, chew, satisfied |
| 28–31 | Drowsy, rest, doze, stretch |
| 32–33 | Husband squats / wife playful crouch |
| 34–35 | Husband repair / wife teasing gestures |

`AnimationPlayer` has per-frame timing, looping clips, held final frames, immediate interruption, and frame deadlines for idle scheduling. The interaction controller still owns the shared timeline; the animation system supplies entry/loop/finish visuals. Props remain separate surfaces and interpolate between owners.

## Documentation sources

- [Android motion sensors](https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion): gravity and rotation sensor choices.
- [Android sensor coordinate systems](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview): natural-device axes must be mapped to display rotation.
- [AOSP AccelerometerPlay example](https://android.googlesource.com/platform/development/+/master/samples/AccelerometerPlay/src/com/example/android/accelerometerplay/AccelerometerPlayActivity.java): screen-axis mapping and gravity-driven physics reference. This app does not adopt that sample's wake lock.

Fold-hinge exclusion, full vendor-specific IME behavior and overnight battery endurance remain hardware acceptance work. No backend, account, pairing or networking was added.
