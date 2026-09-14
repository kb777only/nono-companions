# 0.10.0 — weather animation coverage and umbrella collisions

Rain droplets now collide with the visible top silhouette sampled from each actual umbrella frame. The contour uses the same calibrated position, scale and facing direction as the character. Gaps outside the canopy no longer fall back to an unrelated invisible head-height plane. The rain window may extend beyond display edges so Android cannot independently clamp it away from the character.

Fourteen generated animation atlases add full pose sequences for both spouses: cold, hot and night clothing, plus umbrella variants of default/cold/hot/night clothing. Each source contains 56 poses. Runtime uses 752 of the new frames: 336 seasonal and 416 umbrella frames. The 32 unused umbrella-sheet falling/landing frames are intentionally bypassed. The app now selects 910 unique frames across all 1,024 character/frame/outfit/rain combinations.

Covered clips include walking, blinking, falling/landing in seasonal clothing, dinosaur claws, hugs, reaching/snack theft, eating, resting, personal antics/fixing, kissing, foot kicks, stretching, signature gestures and running. Individual clip frames no longer collapse onto one seasonal idle/eating pose. Parachutes, peeking heads and heat-cooling actions retain their specialized existing artwork. Umbrellas are put away during falling, landing, parachuting and peeking.

Weather selection:

| Condition | Appearance |
| --- | --- |
| Clear, cloud, fog, unknown | Temperature-appropriate normal/cold/hot outfit |
| Snow | Warm clothing, including the boy's plain hood up |
| Rain or storm | Matching integrated umbrella animation set |
| Night | Matching pyjamas, with umbrella when raining |
| Device hotter than 43°C | Hot outfit and existing cooling actions take priority |

The new atlas sheets share a robust head-scale calibration within each outfit so expression/hand changes cannot change the render scale. Complete figures are isolated across uneven generated row boundaries. Original images are retained. A six-atlas memory cache loads artwork on demand.

## Artwork and reproducibility

Built-in image generation was used. Prompts and original outputs are in `art/weather-v10/`; `generation.json`, `cold-revision.json` and `matte-generation.json` record the prompt set and selected results. The first cold-clothing attempt was replaced with a plain hood. Background correction passes preserve cream clothing and white shoes while preparing transparent sprites. Final runtime PNGs are `app/src/main/assets/art/calibrated/w10-*.png`.

Run preparation in order: `prepare-weather-animation.ps1`, `measure-weather-animation.py`, `pack-weather-animation.ps1`, `publish-weather-animation.py`, `preview-weather-animation.ps1`, then `verify-calibrated-art.py`. The Python steps analyze pixels and publish metadata; PowerShell/.NET writes transparent image assets. Retain generated outputs to reproduce the packed atlases without repeating generation.

## Verification and installation

Debug APK build and signature verification passed. All 90 unit tests passed; lint reports zero errors and 20 warnings. Regression tests cover measured rain collisions, facing direction, contour gaps, distinct seasonal/rain clip frames, all pose routes, scale, density, intermediate rotations, bounds and snow/heat precedence. All 910 frame crops passed asset checks; packaged PNG/CSV bytes match the verified files. All 14 new contact sheets were visually reviewed. Reviewed sheet-scale overrides exclude cream shirts from face measurements.

APK SHA-256: `5a9432019866a5416f47423e24548c31ead7a2fa72dc07789b81e8903ecbf5c4`.

Install the debug APK over the existing app to keep local state, then reopen NoNo. Device-specific overlay and keyboard behavior still needs phone verification; no connected device or configured emulator was available. Generated artwork retains some pose-to-pose detail variation and approximate hand contacts. Automatic live weather remains disabled; manual weather controls are available.
