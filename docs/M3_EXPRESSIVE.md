# Material 3 Expressive mobile design

This branch moves NewTube's phone UI from a YouTube-like Material Components shell to the
View/XML implementation of **Material 3 Expressive** shipped by Material Components 1.14.0.

## Principles

- Keep the information density that makes NewTube useful. Video cards stay compact and the
  player remains video-first.
- Use Expressive where interaction matters most: navigation, search, primary actions, settings,
  modal surfaces, and list selection.
- Keep YouTube red for playback/progress identity. Use a softer red/pink Material role palette
  for interactive containers so the UI does not become a wall of bright red.
- Prefer the library's real `Theme.Material3Expressive` and `Widget.Material3Expressive`
  components over hand-drawn approximations.
- Do not couple the redesign to the media/service layer. Everything here stays inside
  `src/stmobile`.

## What changed

- `Theme.NewTube` inherits `Theme.Material3Expressive.DayNight.NoActionBar`.
- Full light/dark Material 3 role palettes are defined for primary, secondary, tertiary,
  container, outline, and surface roles.
- Home navigation uses the Expressive 64dp navigation bar and tonal active indicator.
- Home and Search use Expressive icon buttons; Search gets the 56dp high tonal pill geometry.
- Primary actions use Expressive button size/shape overlays, including shape morphing on press.
- Settings rows are rounded tonal cards rather than a flat preference list.
- Bottom sheets use 28dp top corners and a high surface-container tone.
- Playlist, Cast, sign-in, subscribe, comments/live-chat entry cards, You rows, search
  suggestions, and Cast targets share the same expressive shape/surface language.
- Existing NewTube motion remains in place; Expressive widgets add the Material spring/shape
  behavior supplied by MDC 1.14.0.

## Intentionally unchanged

- Networking, account, SponsorBlock/DeArrow, downloads, and playback engine.
- Video playback chrome stays dark and content-first.
- Video cards remain mostly flat; adding large containers around every thumbnail would reduce
  feed density and work against the product.

## Build

The branch has a dedicated `.github/workflows/m3e-ci.yml` workflow that builds
`:smarttubetv:assembleStmobileDebug` and uploads debug APKs.
