# Weather implementation — provider approval pending

Local outfit selection, rendering and bounded rain effects are implemented. Automatic WeatherClient activation in CompanionService is deliberately not connected pending the user’s answer to the Open-Meteo data-sharing approval question. Version 0.6.0 applies phone-clock pyjamas and local battery-heat overrides automatically. It removes the town-search entry point and INTERNET permission; prepared WeatherClient/WeatherSetup source is unreachable. Do not publish this work-in-progress APK as a complete weather release.

## Proposed provider and privacy

A user manually chooses a town through Open-Meteo geocoding; forecast requests send that town’s latitude/longitude. No GPS permissions are requested. Open-Meteo also receives the normal connection IP address. No screen content, companion state, chat messages or other-app data is sent. The new setup dialog explains this before any town search. No geocoding or forecast request has been run in this session. All builds/tests are offline.

Weather refresh is prepared for 30-minute intervals while the service is awake/unlocked, with 10-minute failure backoff and a three-hour freshness limit. Cached weather persists locally. Temperature is raw temperature_2m Celsius, not apparent temperature. Codes distinguish clear/cloud/fog/rain/snow/storm; snowfall alone does not spawn water rain. Night uses phone local time 22:00–07:00 and takes wardrobe priority except during the battery heat override (>43°C until <=42°C). Cold enters below 12°C and exits at 14°C; hot enters at 26°C and exits below 24°C. Unknown/stale data falls back to normal clothing by day and pyjamas by night.

## Artwork and behavior

Built-in image_gen produced three 4×6 wardrobe atlases, 12 poses per spouse for each outfit, plus two independent umbrellas. Full-body action timing maps onto the dressed poses. Walk, blink, eat, claw, rest, kiss, fall, parachute, reach and working poses are represented; dressed clips use fewer distinct frames than the original art. Peek heads retain original cuffs. Exact hand/umbrella contact and pose consistency need hardware/art review.

Final artwork: app/src/main/assets/art/weather-cold.png, weather-hot.png, weather-night.png, umbrellas.png. Generated sources are retained in art/v2/. Two sheets initially had baked checkerboards; built-in imagegen replaced only those backgrounds with magenta, then the existing deterministic chroma pipeline extracted real alpha. The umbrellas arrived with real alpha and were copied intact.

The rain surface renders in front of the character, within one separate pass-through overlay per spouse. Droplets fade in over 120 ms, bounce once against an approximate canopy/head/body profile, and fade out during their last 250 ms. Walking produces short-lived droplets at the feet; existing particles translate against character motion to remain behind. Population is capped at 60 per spouse and clears on keyboard hiding, screen-off, geometry changes or rain ending. Effect/prop/canopy window opacity is reduced and speech suppressed during rain to limit overlapping non-touchable overlay opacity. This is a localized effect, not a full-screen weather layer.

## Generation prompt specification

Each wardrobe atlas prompt used the approved concept as likeness reference and requested exactly four columns, six rows; husband top three rows and wife bottom three. Poses in order: idle, blink, walking right left-foot-forward, walking right right-foot-forward, empty-hand eating, dinosaur claws, seated rest, right-facing puckered kiss, falling surprise, parachute-harness hands raised, right-facing reach, kneeling work. Adult 2.75-head proportions, same identities/hair/skin/glasses/facial hair, clean cel shading, aligned uncropped feet, no props/text/gridlines. Cold, original prompt before requested correction: cream padded jacket/sage scarf and plum coat/lavender scarf with warm trousers. Hot: ivory dinosaur T-shirt/black shorts and black heart T-shirt/plum shorts. Night: cream dinosaur-pattern and plum heart-pattern two-piece pyjamas/slippers. Background requested transparent, with solid #FF00FF fallback. Correction prompts preserved all characters and replaced only checkerboard background with flat magenta.

Umbrella prompt: two independent open umbrellas side-by-side, cream/sage dinosaur and plum/lavender heart, full canopy, central shaft and hooked handle, matching chibi cel shading, no characters, no rain, text or gridlines.

References: [Open-Meteo forecast fields](https://open-meteo.com/en/docs) and [town search API](https://open-meteo.com/en/docs/geocoding-api).

The husband cold atlas was subsequently regenerated with a cream hood-up hoodie and no scarf in every pose, preserving the wife’s entire outfit. See VERSION-0.6.md for the correction and cooling prompts.
