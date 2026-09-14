Parachute cleanup, keyboard detection and integrated rain artwork.

- Canopies are removed even before window attachment, expire without updates, and close on supporting-edge contact.
- Improved keyboard event/inset fallback and connection cleanup. Onboarding now shows whether Android recognizes, enables and connects the accessibility service, or a manual override is active.
- 96 generated rain poses across four outfits: umbrellas are drawn into the character poses. The separate rain surface draws droplets only. Atlas borders are aligned and unclipped.

After updating: set **Clavier simulé → Auto** in the developer menu. In Android Accessibility, re-enable **NoNo · Détection du clavier** if needed. Return to NoNo to read its diagnostic status.

The v0.7.0 APK still contained the accessibility declaration; the exact vendor Settings-list failure is not confirmed without the phone. No device/emulator test was performed. 73 tests pass; debug build and lint pass with zero errors and 19 warnings. APK signing, service metadata and rain assets verified.

Rain sprites fit the existing small touch windows, so their bodies appear smaller than dry sprites. Auto live weather remains disabled pending the earlier sharing approval; manual Rain previews locally.

SHA-256: 8ef112cbbbc2e9b53f1dd098b6913e3eaa9900a29f977cbd32eecc5e57b8fedc
