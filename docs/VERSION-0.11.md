# Version 0.11.0 — original-style weather artwork

Replaces the rejected 0.10 weather illustrations with 56 smaller sheets derived directly from the original poses, approved concept, and original seasonal outfits. The replacements restore the husband's glasses, moustache and pointed goatee, the wife's layered hair and facial details, the original shading, and muted clothing colours.

Cold, summer, night and their rain combinations retain the regular movement, social, personal and idle pose sets. Rain/default has its own matching set. Two-handed gestures use an umbrella support behind the shoulder, avoiding extra hands. Summer sleeves/shorts and button-up pyjamas were checked and corrected. Parachute, peeking and cooling artwork remains the specialized original artwork.

Transparent sprites are packed into 56 smaller atlases with individual crop and face anchors. A neutral standing pose in each sheet is calibrated against the original face-to-sole distance; this avoids treating a hood or umbrella as extra body height. Whole figures use uniform scaling without stretching. All 1,024 selectors resolve to 910 unique frames across 75 atlases. The existing rain collision samples the visible canopy pixels, including these replacements.

## Reproduce the assets

The built-in image generation tool was used. Source images, original reference collages, the generation plan, and per-sheet prompts/correction prompts are retained in `art/weather-v11`. No image generation API key is needed to use the app.

```powershell
./tools/prepare-weather-v11.ps1
python tools/measure-weather-v11.py
./tools/prepare-weather-v11.ps1 -PackOnly
python tools/publish-weather-v11.py
./tools/preview-weather-v11.ps1
python tools/verify-calibrated-art.py
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --offline --no-daemon
```

Python requires Pillow and NumPy for analysis. PowerShell/.NET performs alpha extraction and atlas packing. Prompts request a flat cyan extraction background; selected output is converted to transparent PNG with edge colour removal. Source backgrounds are never rendered in the app.

Install the new APK over the existing app. Existing settings and moods are retained. If Android has stopped the overlay service, open NoNo and welcome the companions again. No connected phone or configured emulator was available for this update. Generated frame details and exact partner hand contacts can still vary; the automated scale checks do not prove perfect perceptual continuity. Live weather remains disabled, with manual weather available in the developer menu.
