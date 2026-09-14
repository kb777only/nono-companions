# 0.9.1 — consistent character scale

All 296 character frames now use one calibrated rendering path, including every default, seasonal, rain, cooling, parachuting, kissing, idle and peeking pose. The 1,024 possible character/frame/outfit/rain selections have no uncalibrated fallback.

Each spouse has a fixed face-size reference. A seated pose becomes shorter because the legs bend; an umbrella adds height without shrinking its owner. Horizontal and vertical scale are identical. Keyboard and fold bounds translate characters instead of reducing their size. Peeking heads retain their aspect ratio.

The original artwork is preserved. Nineteen cleaned atlases isolate complete figures across the original grid boundaries and remove neighbouring hands/shoes. Metadata identifies the source rectangle, face anchor and reviewed scale for each frame. Automatic skin measurements were visually reviewed and corrected where hands, clothing or facial expressions affected them. All 19 calibrated contact sheets were inspected after isolation; the final review also corrected a cold-rain walking frame and two hot-outfit action frames.

Drawing space is separate from the small touch window. Extended arms and umbrellas render on a non-touchable overflow surface. Both surfaces share an integer center and complementary clipping. A conservative combined-opacity budget keeps pass-through effects below Android's obscuring limit, even if the spouses overlap. Overflow portions may therefore look more translucent than the interactive core. This follows [Android's overlay touch rules](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_NOT_TOUCHABLE).

Hearts, sweat, dialogue and held props follow the calibrated face position. Rain collision height follows the integrated umbrella bounds. Parachute placement follows the adjusted face height.

## Verification

- `CharacterGeometryTest`: all 1,024 routes and 296 records; six densities; uniform scale; complete source bounds; drawing bounds; every five degrees of rotation with both facing directions; edge placement across nine display sizes.
- `OverlayOpacityTest`: up to twelve simultaneous pass-through layers stay below combined opacity 0.78.
- `tools/verify-calibrated-art.py`: all 296 nonempty transparent crops, padding, packaged PNG hashes, metadata agreement and drawing bounds pass with zero failures. Machine-readable results: `scale-audit/calibrated-verification.json`.
- All 19 atlases were visually reviewed as calibrated contact sheets. This checks visible character consistency beyond the mathematical calibration; it is not a guarantee of identical anatomy in independently generated drawings.

The existing v0.9.0 failed audit remains as historical evidence, not the current result. The new tests exercise the real runtime selector rather than the old audit's duplicated selector.

## Reproduce artwork preparation

Run in order from the project root:

1. `python tools/calibrate-characters.py`
2. `python tools/isolate-character-frames.py`
3. `./tools/isolate-character-frames.ps1`
4. `python tools/publish-character-calibration.py`
5. `./tools/preview-calibrated.ps1`
6. `python tools/verify-calibrated-art.py`

Python requires Pillow and NumPy. PowerShell extraction uses System.Drawing and copies original pixels without resizing. Manual measurement reviews are in `art/v2/face-regions.json` and `art/v2/reviewed-face-spans.json`. Do not build between calibration and publishing: publishing produces the final packed source coordinates.

## Installation and practical limits

Install the release APK over the existing app to retain local state. Reopen NoNo afterward. If automatic keyboard detection is absent, re-enable the optional NoNo Accessibility service; some sideloaded installations require Android's restricted-settings approval first.

No phone is connected and no emulator is configured. Actual touch routing, translucent overflow seams, keyboard handling and folded/unfolded displays still require hardware verification. Very small usable areas cannot contain a full-size figure; the app never shrinks it to force a fit. Existing generated detail drift and approximate hand contacts remain. Automatic live weather remains disabled; manual weather is available.
