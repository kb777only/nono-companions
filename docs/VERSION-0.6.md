# Version 0.6.0 — local heat and cooling antics

When Android reports battery temperature above 43.0°C, both companions switch to their light T-shirt/shorts outfits, even at night or in cold weather. They return to the ordinary outfit rules at 42.0°C or below, avoiding rapid threshold flicker. Battery and weather readings remain separate values. Unavailable or implausible battery values clear the override; no temperature is invented.

The service receives the sticky `ACTION_BATTERY_CHANGED` broadcast and its updates. `EXTRA_TEMPERATURE` is divided by 10 without rounding before comparison. This is **battery temperature**, not CPU or phone-surface temperature. No extra permission, polling, root, private sensor files, logging or network transmission is used. Android's [BatteryManager API](https://developer.android.com/reference/android/os/BatteryManager#EXTRA_TEMPERATURE) documents the field; the [Android battery service](https://android.googlesource.com/platform/frameworks/base/+/android16-qpr2-release/services/core/java/com/android/server/BatteryService.java) supplies `batteryTemperatureTenthsCelsius`.

## Cooling sequence

Each eligible grounded character fans themselves for six seconds, kneels to lower a desk fan for 900 ms, switches it on after another 700 ms, enjoys nine seconds of breeze, then puts it away for seven seconds. The sequence repeats while hot. The housing stays still while an independent rotor spins behind its transparent grille, with a small lit switch. Folding fans have separate pivots and sway with the wrist poses. Three staggered sweat droplets fade and slide around each face. These are cosmetic effects and do not cool the phone itself.

Touch reactions, dragging, falling, parachuting, keyboard retreat, display changes, screen-off and explicit shared actions interrupt cooling and clear its props. Cooling retries after a short delay when grounded and eligible. Automatic exercise and other random scenes wait while hot; menu actions remain available. Cooling rendering is limited to the two existing character windows and an 80 ms schedule when no faster physics is needed. There are no new invisible touch regions or full-screen effects. Screen-off stops rendering.

Night pyjamas use the phone's local clock from 22:00 to 07:00. Live weather is **not enabled**: the earlier automatic approval review blocked sending selected-town coordinates to Open-Meteo pending explicit user approval. The prepared provider source remains unconnected, its setup entry point is removed and this APK has no INTERNET permission. Cold/hot weather rules and rain/umbrella particles are implemented and tested with synthetic readings, but no current weather is fetched. See [weather work in progress](WEATHER-WIP.md).

## Generated assets

- `app/src/main/assets/art/weather-cold.png`: 4×6 atlas; all 12 husband poses now use a cream hoodie with the hood up and no scarf. Wife's warm outfit retained.
- `app/src/main/assets/art/cooling.png`: 4×2 atlas; four additional warm-weather poses per spouse: wrist low, wrist raised, kneeling place/switch, seated in breeze.
- `app/src/main/assets/art/cooling-props.png`: 2×3 atlas; husband left/wife right, folding fans first row, empty desk-fan housings second, independent rotors third.
- Hot and night outfit atlases and two umbrellas from the preceding weather work are included. Original generated source PNGs remain in `art/v2/`.

Built-in image generation used the existing wardrobe as the likeness reference. The hood edit prompt required unchanged canvas, four columns/six rows, matching poses and wife unchanged; only the husband's scarf/jacket was replaced by a cream dinosaur hoodie, hood up in every pose. The cooling pose prompt required four columns/two rows, the exact T-shirt/shorts outfits, aligned complete bodies, empty hands for independent props, consistent adult chibi proportions and a flat magenta background. The prop prompt specified matching cream/sage and black/plum folding fans, front-facing empty fan housings and separate three-blade rotors. A follow-up replaced the accidentally generated gradient background, including grille holes, with flat magenta. `tools/prepare-art-v2.ps1` converts that background into real alpha without altering the source files.

## Validation and limits

Debug assembly, all 59 unit tests and Android lint pass (zero lint errors; seven existing warnings). Eight new tests cover the exact raw threshold, hysteresis, invalid sensor data, precedence over cold/night, rain independence, placement/switch/cleanup timing, retry delay, drag, keyboard, release/falling, cooling exit and resize interruption. The processed art has real transparency and nonempty content in all 38 new/replaced cells. APK contents are checked before release.

No new device or emulator test was performed: the phone is disconnected and no emulator is configured. Samsung/Xiaomi battery reporting, exact hand-to-fan contact, small-size readability, rotation, fold and keyboard behavior still need hardware verification. Generated poses retain some visual drift; these are four cooling poses with independent animated props, not a full new multi-frame animation for every outfit/action. Peek heads retain their existing appearance. The cold hoodie is ready in the wardrobe, but automatic cold-weather selection awaits live-weather approval.

Install the debug APK from the private GitHub release over the existing app, then open NoNo once. Existing overlay and optional Accessibility permissions remain the setup controls. No backend, account or remote control is included.
