# Version 0.5 — keyboard retreat and deliberate orientation

When the keyboard is reported visible, both characters end shared actions, put props away, clear speech and retreat to opposite screen edges. Generated heads and hands peek inward and blink until the keyboard closes. Their windows become pass-through immediately while typing. The available area is clamped above the keyboard; when less than 60 dp remains, the characters are temporarily invisible instead of covering a full-screen keyboard. On closing, they move back inward and resume gravity and normal behavior. This version chooses the requested retreat-and-peek behavior rather than adding a second jumping behavior.

## Keyboard detection and privacy

Enable **Optional app & keyboard awareness** from the app. Android may require turning the accessibility service off and on after updating its capabilities. Onboarding now explains the change: the service uses only window types, IDs and bounds to identify input-method windows, plus foreground app categories. It never requests nodes, roots, field text or typed content, and does not operate other apps or transmit data. Android requires canRetrieveWindowContent and flagRetrieveInteractiveWindows to enumerate these window records; that capability is broader than the data actually used.

Without Accessibility, the existing overlay IME insets remain a fallback. Some vendors/apps do not expose another app’s keyboard to overlay insets, so detection cannot be guaranteed without the optional service. Empty/protected window lists fall back to insets. Floating keyboard bounds conservatively reserve the area below their top. Hardware keyboards with no visible IME require no retreat. Detection is event-driven with a 120 ms bounded coalescing delay; there are no accessibility-tree scans or background polling. Moves of the companion windows do not schedule keyboard inspections.

## Orientation

The preferred sensor is Android’s game rotation vector, which fuses gyro motion with accelerometer reference. Gravity and accelerometer sensors are fallbacks. The sensor axes are remapped into the current display coordinates. A new quarter turn requires moving at least 55 degrees away from the current target and holding the new quadrant for 250 ms. Turns use a shortest-path smoothstep over 450 ms, or 650 ms for a half-turn. Targets are 0, 90, 180 (equivalent to -180), and -90 degrees. Nearly flat or unreliable readings retain the last deliberate orientation.

Characters, their held props and parachutes rotate together; bubbles and action-menu text remain upright. The character’s rotated bounding rectangle is recomputed during a turn, so sprites are not clipped by the original portrait window. Physics bounds follow the same footprint. Rendering speeds up only while moving/turning, and sensors stop screen-off. The generated peek heads also rotate and keep small pass-through windows within the usable area.

## Art and validation

Built-in image_gen generated art/v2/peeking-source.png using the approved concept for identity. The existing extraction tool produces app/src/main/assets/art/peeking.png. The atlas contains open-eye and blinking heads/hands for each spouse, mirrored for the opposite edge. The full runtime set now has 44 pose/animation frames per spouse.

Prompt: “Create a mobile game sprite atlas, EXACT 2 columns and 2 rows equal square cells, 1024x1024. TOP ROW husband, BOTTOM ROW wife. Each cell contains ONLY a close-up chibi head plus two small hands peeking around an invisible vertical screen edge on the LEFT of the cell, body hidden outside screen. Head tilts playfully to RIGHT looking inward with curious affectionate expression, tiny fingers gripping invisible edge; NO rendered wall or border. Column1 eyes open observing; Column2 same perfectly aligned head eyes gently closed blinking. Husband distinctive short receding brown hair round glasses long nose thin moustache pointed goatee light skin; cream dinosaur hoodie cuffs only visible. Wife dark shaggy hair curtain bangs smoky eyes tiny silver nostril hoop soft cheeks, black tactical jacket cuffs. Preserve adult spouse likeness and clean anime-chibi soft cel shading from reference. Head scales identical across two frames; hair fully uncropped with clear margins; no torso, text, labels, props or background. Genuinely transparent alpha background. If alpha cannot be produced, use pure uniform MAGENTA #FF00FF for clean extraction; no checkerboard or grid lines.”

New automated checks exercise jitter rejection, threshold dwell, quarter/half-turn easing, wraparound, flat-device handling, rotated footprints, keyboard interruption/quiet waiting/return, rapid visibility changes, geometry changes, and metadata/inset fallback. The phone remains disconnected; vendor keyboard detection, smoothness, visual contacts and Fold transitions have not been tested on hardware.

## Official references checked

- [Android position sensors](https://developer.android.com/develop/sensors-and-location/sensors/sensors_position): game rotation vector and sensor fusion.
- [Sensor coordinate systems](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview): natural-device axes and display remapping.
- [AccessibilityServiceInfo](https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo): interactive-window capability requirements.
- [AccessibilityWindowInfo](https://developer.android.com/reference/android/view/accessibility/AccessibilityWindowInfo): input-method window type and screen bounds.
