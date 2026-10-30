# LinkGrab UI: "Bold Sticker"

Keep new screens in this style. All tokens live in `ui/theme/Sticker.kt` and `ui/theme/Theme.kt`.

## Tokens
| Name | Value | Use |
|---|---|---|
| Yellow | `#FFD93B` | Screen background |
| Ink | `#000000` | Text, borders, shadows, primary buttons |
| White | `#FFFFFF` | Cards, inputs, unselected chips |
| Pink | `#FF6B9D` | Error cards, big-button shadow, selected size-limit chip |
| Blue | `#2F5BFF` | Selected VIDEO/AUDIO chip, GO button (white text) |

- Border: 3dp solid Ink on every card, input and button.
- Shadow: hard offset, no blur (`Modifier.stickerShadow`): 4dp on cards, 3dp on chips, 6dp pink on the DOWNLOAD NOW button.
- Radii: cards and inputs 16dp, big button 18dp, chips 14dp, pill 20dp.
- Type: sans-serif, Bold for headings and buttons. Headline 56sp / line height 54sp / letterSpacing -2. Swap in Space Grotesk by editing `LinkGrabFont` in `Theme.kt`.
- Always light theme. Dark mode and dynamic color are intentionally off.
- Touch targets are at least 44dp. Icon-only buttons need a content description.

## Building blocks
- `Modifier.sticker(background, radius, shadowOffset, shadowColor)`: fill + border + hard shadow
- `StickerButton`: main button (use `container`/`content` to recolor)
- `StickerChip`: selectable toggle (blue when selected)
- `StickerPill`: small status label

## Notes
- Android draws elevation shadows blurred, so the hard shadow is drawn behind the element. Leave at least 4dp of padding around anything with a shadow, or the shadow gets clipped (see `contentPadding` in `HomeScreen`).
- History, Downloads and Settings screens inherit the colors and shapes from the theme but have not been re-laid-out yet.
