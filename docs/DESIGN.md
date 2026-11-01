# LinkGrab UI: "Bold Sticker"

Keep new screens in this style. Tokens and components live in `ui/theme/Sticker.kt` and `ui/theme/Theme.kt`.

## Palette (light / dark)
| Token | Light | Dark | Use |
|---|---|---|---|
| `Sticker.Yellow` (ground) | `#FFD93B` | `#121212` | Screen background |
| `Sticker.Ink` | `#000000` | `#FFD93B` | Text, borders, primary buttons |
| `Sticker.White` (card) | `#FFFFFF` | `#1E1E1E` | Cards, inputs, unselected chips |
| Shadow | `#000000` | `#FF6B9D` | Hard offset shadow |
| `Sticker.Pink` | `#FF6B9D` | same | Warnings, selected size chip, error cards. Text on it: `Sticker.Black` |
| `Sticker.Blue` | `#2F5BFF` | same | Selected chips, running progress. Text on it: `Sticker.OnBlue` |

Rule: never put `Sticker.Ink` text on Pink or Blue (it turns yellow in dark mode). Use `Sticker.Black` / `Sticker.OnBlue`.

## Shape and motion
- 3dp border on every card, input and button; hard (un-blurred) offset shadow, 4dp on cards, 3dp on chips, 6dp pink on DOWNLOAD NOW.
- Radii: cards 16dp, big button 18dp, chips 14dp, pills 20dp.
- Press feel: a pressed `StickerButton` slides onto its shadow (60ms). `haptic = true` adds a buzz (used on DOWNLOAD NOW).
- Grab moment: tapping DOWNLOAD NOW pops a pink "GRABBED!" pill in the header; a finished download pops "SAVED!" (`PopPill`).
- Library cards tilt +/-1.2 degrees like stickers on a wall.

## Building blocks
`Modifier.sticker(...)`, `StickerButton`, `StickerChip`, `StickerPill`, `PopPill`, `StickerCard`, `StickerSectionTitle`, `stickerSwitchColors()`.

## Theme choice
Settings > Appearance: AUTO (follow the phone), LIGHT, DARK. Stored by `ThemePrefs` (SharedPreferences `linkgrab_ui`).

## Splash
Android 12+: system splash with `linkgrab_icon_fg` on `linkgrab_window_bg` (yellow, or `#121212` in night mode). Older Android: `linkgrab_splash_bg` layer-list. Both set in `values/themes.xml` and `values-v31/themes.xml`.

## Notes
- Leave at least 4dp of padding around anything with a shadow or the shadow gets clipped (use `contentPadding`, not `padding`, on lazy lists).
- Font is the system sans-serif Bold. To use Space Grotesk, add the .ttf files to `res/font` and edit `LinkGrabFont` in `Theme.kt`.
- Accessibility: touch targets at least 44dp, AA contrast on both themes.

## Grab moments (update 4)
- **Grab:** DOWNLOAD NOW buzzes, the preview card slides down and fades into the queue (about 320ms), the header pill pops "GRABBED!".
- **Saved:** when a download finishes, a big tilted pink "SAVED!" sticker pops in the middle of the screen (`SavedSticker`) and the header pill pops "SAVED!".
- **Clipboard banner:** a blue `ClipboardBanner` ("LINK FOUND. GRAB?") replaces the old auto-paste. GRAB fills the link and fetches the preview; X dismisses it. It re-checks every time the app comes to the front.
- **Loading:** `FetchingArrow` (an arrow bouncing onto a pink tray, "FETCHING…") replaces the plain progress bar while a link is checked.

## Notifications
- The download notification shows the video title, a real progress bar, and "percent · speed · time left", with a yellow accent and the arrow icon. It updates about once a second without sound.
- The notification permission (Android 13+) is requested the first time you tap DOWNLOAD NOW if it is missing, and once after the welcome dialog.

## Player (update 5)
- **Where:** `ui/player/` (PlayerScreen, MiniPlayer, PlayerUi, PlayerParts), `playback/` (PlaybackService, PlayerConnection, PositionStore).
- **How it works:** the real ExoPlayer lives in `PlaybackService` (a Media3 session), so audio keeps playing with the app closed and the notification and lock-screen controls come for free. The UI talks to it through a `MediaController` held by `PlayerConnection`.
- **Library:** tap a card to play it (the whole filtered list becomes the queue), hold for PLAY / SHARE / OPEN IN ANOTHER APP / DOWNLOAD AGAIN / DELETE. A card shows live equalizer bars while it plays.
- **Video:** immersive full screen, controls hide after 3 seconds. Double-tap left/right = -10s/+10s, swipe up/down on the right = volume, on the left = brightness. Buttons: minimize, picture-in-picture, rotate.
- **Audio:** tilted artwork card that breathes while playing, equalizer bars, seek bar, previous/next, repeat (off / all / one), shuffle, speed (0.5x to 2x), sleep timer (15 / 30 / 60 min).
- **Mini-player:** sticker bar above the bottom nav on every tab; tap to open, X to stop.
- **Resume:** the position of every file is remembered (not for the first or last 5 seconds).
- Press-feel icon buttons are `StickerIconButton`; the seek bar is `StickerSeekBar`.
