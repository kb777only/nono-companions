# Version 0.8.0 — parachute cleanup, keyboard diagnosis and integrated rain art

## Parachutes

Tracked canopy windows are now removed immediately even if Android has not yet marked the view attached. Previously, skipping removal in that interval could leave a window outside the service's tracking map. Each canopy also has a renewable 500 ms lease: without another active glide update it becomes invisible. Detachment clears pending callbacks. Character and canopy drawing explicitly clear the transparent surface before repainting.

Supporting-edge contact ends the parachute state even if tangential velocity remains too high to count as fully settled. Rendering checks the current state, fall timer, support and held character instead of relying on the state enum alone. Dragging, keyboard retreat, resize, screen-off, lock and service destruction still clear transient windows.

## Keyboard and Accessibility

The v0.7.0 **packaged APK** was inspected: it still contains the exported ContextService, BIND_ACCESSIBILITY_SERVICE permission, accessibility intent and metadata resource. There was no missing service declaration introduced by the developer menu. The exact reason for a vendor Settings list omitting the service cannot be established without the affected phone.

This version labels the service **NoNo · Détection du clavier**, explicitly enables its component, adds its settings activity, and reapplies interactive-window flags, event types and the unfiltered package configuration when connected. Window detection now considers relevant bounds/focus changes in addition to add/remove, performs one delayed settling check after window-state events, and refreshes on companion startup/screen wake. Known non-IME window movement and NoNo's own events are filtered to avoid repeated scans from overlay motion. Disconnect/destruction clears cached keyboard state and signals fallback detection.

Positive keyboard insets now remain usable when a prior accessibility snapshot reports no keyboard. Main onboarding reports whether Android recognizes the service, whether it is enabled and connected, and whether the developer menu is manually overriding keyboard state. It also exposes the installed app version, the standard Accessibility page and Android app information page. No permissions are enabled programmatically and no protected-screen behavior is bypassed.

After updating, set **Menu développeur → Clavier simulé → Auto**. Open **Détection automatique du clavier · Accessibilité**, find **NoNo · Détection du clavier**, and re-enable it if necessary. If Android restricts the sideloaded app's settings, use **Informations Android de NoNo** to inspect the platform's restricted-settings authorization. Return to NoNo and read the diagnostic line. If it still reports the service absent or disconnected, that precise status is needed for the next device investigation; reinstalling or changing the manifest cannot guarantee a vendor permission fix.

No text, nodes, password fields or accessibility tree are read. This uses [Android's documented window metadata capability](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService) and standard settings controls.

## Rain artwork

Four new 4×6 RGBA atlases contain 12 integrated character-and-held-umbrella poses per spouse, for default, cold, hot and night outfits: idle, blink, two walk phases, eating, claw, rest, kiss, surprise, relaxed seated, reach and kneeling work. `rain-default.png`, `rain-cold.png`, `rain-hot.png`, `rain-night.png` live in `app/src/main/assets/art/`; sources remain in `art/v2/`. RainView draws **only particles**. The old standalone umbrella asset is no longer loaded or drawn. Flight/landing and keyboard peek poses retain priority over umbrella poses.

Whole rain sprites fit within the existing small character window, keeping the canopy attached to the drawn hand during pose changes. This makes rain characters visually smaller than dry sprites rather than enlarging the touch-blocking rectangle. Rain collision uses the new canopy location inside that window; sweat and kiss hearts adjust to the rain face position. Existing separate snacks/tools/fans remain independent props.

Built-in image generation was used. The final prompt set requested exact 1024×1536 / four-column-six-row atlases, husband top three rows and wife bottom three, preserved approved identities, two arms per character, complete open umbrella with continuous shaft-to-hand contact, and the pose order above. Default rain was corrected to remove baked cookies and a misplaced identity. That corrected layout was reused for wardrobe-only edits, preserving poses and alignment. Cold uses a plain cream hood-up hoodie without a scarf for the husband and plum coat/lavender scarf for the wife; hot uses their T-shirts/shorts; night uses their patterned pyjamas/slippers. A final cold correction removed invented animal-hood decorations. Transparent backgrounds were requested; accepted outputs used solid magenta, converted to real alpha by the existing deterministic preparation tool. Rejected drafts are not runtime assets. Measured connected-figure row bounds in rain-layout.json and align-rain.ps1 repack the accepted artwork at a shared scale/foot anchor, removing generated cell-boundary drift. All 96 final cells have transparent borders with no neighboring frame bleed.

Generated pose drift, limited distinct walk phases and approximate contacts remain. These are integrated action poses, not a claim of complete studio-quality animation. Live weather is still disabled pending the previous Open-Meteo approval; manual Rain in the developer menu previews everything locally.

## Verification

Regression tests cover expiring canopies, unsettled supporting-edge contact, stale-state rejection, positive keyboard-inset fallback and rain pose priority/mapping. Debug build, test count, lint and APK inspection results are recorded in VERIFICATION.md. No affected-phone or emulator test is claimed: the phone remains disconnected and no emulator is configured. The vendor Accessibility-list issue requires confirmation after installation.
