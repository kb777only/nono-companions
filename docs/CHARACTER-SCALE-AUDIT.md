> Historical v0.9.0 failure report. The v0.9.1 fixes and replacement verification are documented in [VERSION-0.9.1.md](VERSION-0.9.1.md) and [calibrated-verification.json](scale-audit/calibrated-verification.json). The old script is intentionally pinned to the old renderer.

# Character scale audit — v0.9.0: FAIL

Audit date: 2026-09-14. Application commit: 218493b. No app or artwork correction is included in this verification change.

The requirement of uniform perceived character size, without exceptions, is **not met**. The prior 80 passing behavior tests and transparent-border checks did not establish that requirement. In particular, packing atlas cells to the same dimensions is insufficient when the person occupies a different proportion of each cell.

## Exhaustive geometry inventory

`tools/audit-character-scale.py` reads the actual shipped PNGs and reproduces the v0.9.0 `PetView` source selection, trimming, destination scaling and view clipping at the canonical 72×104 dp size. It enumerates both characters × 64 frame IDs × four outfits × rain on/off: **1,024 cases**, reaching **296 distinct character cells in 19 atlases**. Peeking, cooling, kiss and parachute character frames are included. Duplicate routes are intentionally retained to detect outfit/state interactions.

- **32 cases** use nonuniform X/Y scaling: the four peeking cells repeated across outfit/rain combinations. A square source cell is stretched into 38×44 dp; the vertical scale is about 15.8% larger than the horizontal scale.
- **221 cases** place some opaque source pixels outside the character view; **190 cases** place at least 1% of those pixels outside it. These counts represent rendering routes, not 221 distinct poses. Opaque here means alpha greater than 40, matching the existing trim threshold.
- The new idle sources are repacked at 0.84 scale, then displayed using the full 384-pixel cell height. They are not calibrated to the original character's head or body dimensions.
- Rain fits the whole umbrella-and-person cell into 72 dp width. This directly reduces the person when compared with a dry pose. The integrated umbrella must not determine the person's size.
- Original atlases trim alpha bounds while later atlases retain their whole cells, producing different treatment of padding and alignment.

Selected **unclipped opaque extents** computed by the renderer model:

| Standing example | Husband height | Wife height |
| --- | ---: | ---: |
| Original default idle, frame 4 | 95.85 dp | 92.23 dp |
| New default stretch-ready stance, frame 52 | 80.65 dp | 79.71 dp |
| Default rain idle, including the entire umbrella | 64.97 dp | 66.09 dp |

The new standing stance is 15.9% shorter for him and 13.6% shorter for her. The umbrella examples are not measurements of the person alone: the **entire** umbrella-and-person artwork is already smaller than the dry person. Visual inspection confirms the head/body shrinkage. Seated and bent poses are naturally shorter, so total opaque height must not be used to normalize every pose.

## Review artifacts and limits

Run `python tools/audit-character-scale.py --strict` with Pillow and NumPy. Exit 1 means geometric failures; exit 2 would mean geometry alone still cannot certify perception. The script is read-only with respect to assets and app code. It writes:

- `docs/scale-audit/index.html`: self-contained viewer of every rendering combination with the actual v0.9 scaling, drawing-window boundary and baseline. Includes a standing-pose comparison button.
- `measurements.csv` / `measurements.json`: all source rectangles, scales, opaque extents and clipped-pixel counts.
- `asset-hashes.json`: SHA-256 identities of all 19 input atlases.

The viewer is enlarged 2× for inspection; this does not represent a larger in-app pet. Its geometry models upright rendering, before Android density rounding. It does not certify device screenshots, all intermediate rotations, touch behavior, or perceptual head/body landmarks. Rotation is an isometric transform in the renderer, but window rounding/clipping still needs separate device verification. No device/emulator playback test is claimed.

Independent snacks, menus, speech skins, fans and the separately drawn parachute canopy do not define character scale and are not among the 296 character cells. Their attachment/contact positioning needs checking after the character calibration changes; keeping their dimensions alone cannot prove that contacts remain aligned.

## Required correction before approval

Use reviewed per-frame head/body landmarks and a shared per-character reference size, preserving aspect ratio and relative spouse size. Align support/contact anchors separately from pose height. Seated poses must retain the same head scale; umbrellas and other props must not shrink their wearer. Rain may need a larger non-touchable drawing region while retaining the small character touch region. Peeking needs the same calibrated head scale with uniform X/Y scaling. Existing seasonal cells also need clipping and padding review. Re-run this inventory and visually check transitions, then test on a device.

No passing verdict, corrected APK or new release is issued by this audit.
