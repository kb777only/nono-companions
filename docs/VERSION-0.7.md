# Version 0.7.0 — developer controls

Open NoNo and tap **Menu développeur**. Each variable has its own Auto/Manuel switch. Manual values take effect after a short 120 ms input debounce and persist locally across app/service recreation. **Tout remettre en Auto** clears all overrides. These controls affect the companions' inputs, never the actual phone temperature, clock, display rotation or other apps.

| Variable | Manual choices |
| --- | --- |
| Weather state | Sun, cloud, fog, rain, snow, storm, unknown |
| Battery temperature | −10 to 80°C, 0.1°C increments |
| Outdoor temperature | −40 to 55°C, 0.1°C increments |
| Time of day | Fixed hour 00–23 |
| Orientation | 0°, 90°, 180°, −90°; matching gravity and smooth turns |
| Keyboard | Closed or open, simulated height 40% of display |
| App context | Unknown, reading, game, media, work |

Temperatures and time offer sliders and **Saisir une valeur précise**; decimal commas and points are accepted. Invalid or out-of-range input is rejected. Disabled values under Auto are labelled as manual preview values, not live sensor readouts. Individual Auto switches remove their overrides; reopening a disabled control uses its default manual preview value.

Auto uses the real battery sensor, sensor orientation, phone clock and existing optional keyboard/context detection. Real sensor values continue to update behind manual overrides, allowing Auto to restore the latest input. Actual battery reporting is still battery temperature, not CPU or skin. Manual weather state and temperature are independent: rain can be previewed with unknown outdoor temperature, and cold clothing can be previewed without inventing weather conditions. Heat priority and hysteresis remain active: above 43°C selects light clothing, ending at 42°C or below, even with manual night/cold values.

Live automatic weather remains unconnected pending the existing Open-Meteo coordinate-sharing approval. This update requests no networking permission and manual weather is fully local. It makes the prepared cold hoodie, umbrellas and rain effects available to preview. Screen-off, lock/protected-screen handling and permission revocation remain enforced. Context dialogue retains normal cooldowns, so changing a context does not guarantee an immediate speech bubble.

The menu is a separate non-exported native activity accessible only from the app, with no floating toolbar or pause/hide button. Keyboard simulation uses the existing retreat/peeking controller and resumes real keyboard bounds when returned to Auto. Display and fold behavior still need hardware verification.

Validation: debug build, unit tests and lint; nine new tests cover input parsing, malformed preferences, independent/persistent override decoding, Auto restoration to current inputs, weather independence, heat priority, orientation/gravity alignment and easing, keyboard and context mapping. No device/emulator UI test was possible because the phone is disconnected and no emulator is configured.

Install the v0.7.0 APK over the current app, open NoNo, then choose **Menu développeur**. Suggested cold preview: manual hour 12, battery 35°C, outdoor 5°C. Rain preview: manual weather Pluie. Heat preview: battery 45°C. Restore Auto when finished.
