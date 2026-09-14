# Version 0.9.0 — everyday life together

Four solo performances per spouse use 32 newly generated transparent frames:

| Husband | Wife |
| --- | --- |
| Seated impatient feet and a hungry belly grin | Alternating boot kicks and a coy seated smile |
| Sporty overhead and side stretches | Gentle sleepy stretches |
| Thinking, adjusting glasses, then a satisfied idea gesture | Hair tuck, small wave, then a mischievous wink |
| A short out-and-back snack mission jog | A playful sneaky run |

Three shared scenes use the existing interaction controller: a seated foot-kicking duet, copying each other's stretch, and a brief out-and-back chase. They approach, acknowledge the invitation, join after a deliberate delay, then exchange smiles. Either spouse can lead. Shared completion increases the existing warm relationship state.

Solo clips enter from idle, perform, settle, and finish; run paths actually move along the floor. Personal cooldowns are 55–95 seconds with quiet gaps between activities. Energy, hunger, affection and mischief weight choices. Tired characters prefer quiet gestures, and rain/night suppress autonomous solo exercise and running; the chase also declines in those conditions. Short French shared dialogue is occasional, with the existing conversation rate limit. No extra prop or bubble surface is added for these performances.

Drag, keyboard retreat, resize, loss of ground support, rotation and heat cancel solo performances. Shared scenes retain the existing cancellation and cooldown rules. Heat and held characters reject new scenes. Rendering wakes at the clip's next frame; movement uses the existing active cadence, and screen-off suspension is unchanged.

**Menu développeur → Petites animations** contains eight solo and three shared preview buttons. They use the same validated handlers as autonomous behavior and can decline while busy, cooling, airborne, on cooldown, or in a quiet weather/time state. No persistent manual animation override or floating toolbar was added.

## Artwork and reproducibility

Source of truth: `art/approved-concept.png`. Built-in image generation produced separate 1024×1536, four-column/four-row atlases for each spouse. Source images are `art/v2/husband-idle-source.png` and `wife-idle-source.png`; runtime images are in `app/src/main/assets/art/` with the same names minus `-source`. `tools/prepare-idle.ps1` removes the flat magenta background, decontaminates its fringe and repacks complete measured row regions at a common scale and foot anchor. This is deterministic asset preparation, not procedural redrawing of characters.

Prompt specification: preserve approved adult chibi identities, proportions, cream dinosaur hoodie/glasses/goatee for him and dark shag/tactical outfit/platform boots for her; four ordered full-body frames for each of seated kicks, overhead/side stretches, signature gestures and running; clean outlines, soft cel shading, no text, props, shadows or grid, generous cell margins, consistent head size and feet alignment. The wife's first draft contained an opaque checkerboard; a second image edit replaced that background with solid magenta and corrected the alternate raised boot. Only the corrected version is shipped.

Frames 48–51 are feet, 52–55 stretch, 56–59 signature and 60–63 run. Seasonal and umbrella outfits reuse their existing compatible seated, reach, claw, blink and walking poses rather than switching back to everyday clothing. **Those outfits do not yet have the full 32 newly drawn idle frames.** Generated running phases remain fairly close and hand/detail continuity is approximate; these are small sprite clips, not interpolated skeletal animation.

## Delivery and verification

Install the v0.9.0 debug APK over the existing app to preserve its local state and permissions. Open NoNo after updating if Android has stopped the service. Existing overlay, keyboard and Accessibility setup remains unchanged. The developer menu provides immediate previews when the characters are available.

Regression coverage includes all eight solo performances, clip advancement, real bounded run movement, quiet mood choices, each interruption source, both leaders for all three shared scenes, cooldowns and heat priority. Final build/test/lint and device availability results are recorded in `VERIFICATION.md`. No live weather access was enabled.
