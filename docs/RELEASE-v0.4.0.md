Version 0.4.0 adds personalized parachutes that open after **0.65 seconds of falling**. Both characters switch to generated gliding poses and descend more slowly, following the phone’s gravity direction. Dragging closes that character’s canopy; landing resets the timer.

Also includes French dialogue, smaller companions, kissing with short-lived floating hearts, character-specific long-press menus, snack theft and persistent local moods.

### Install

Download **NoNo-Companions-debug.apk** below on your phone, open it and accept Android’s update prompt. This uses the same local debug signing key as the previous installed version. For a first installation, open the app and allow it to display over other apps; Accessibility awareness is optional. Sign into the GitHub account that can access this private repository to download assets.

### Validation and limits

Debug build, **29 unit tests**, and Android lint passed (zero errors, six non-blocking warnings). The phone was disconnected, so this release has **not** been installed or visually tested on hardware. Precise canopy/hand alignment and Fold behavior still need device verification. Generated motion is a limited-frame set, not studio-quality animation.

The artwork ZIP contains the transparent runtime atlas set and manifest, including the two new parachutes and gliding poses.

APK SHA-256: `DAD98C272A1381D4B8968198B602C9B045D70F3181EAD7CE6E9A5D31ABA38FE4`
