# Avatar customization plan

## Goal

Let players make a recognizable character from the game's eight existing shapes while keeping joining fast for people who want to use a preset. The Android and web clients should show the same customized avatar anywhere a player appears.

## Player experience

1. Keep the current eight character choices as ready-to-use presets.
2. Add a **Customize** action that opens an editor with a live preview.
3. Let players choose a base shape, a color from the brand palette, eyes, mouth, and one accessory (or none).
4. Provide **Randomize**, **Reset**, and **Save** actions. A player can skip customization and join immediately.
5. Keep the chosen avatar stable for the full room/game so friends can identify its owner.
6. Use selected state, shape labels, and accessible descriptions in addition to color so choices remain distinguishable.

## Shared avatar configuration

Represent customization with five small stable IDs:

- `shapeId`: one of `sunny`, `lime`, `violet`, `tangerine`, `cloud`, `star`, `berry`, or `mint`.
- `colorId`: one of the eight curated avatar colors: `sunny` (#FFAE29), `lime` (#B6F45B), `violet` (#A394F1), `tangerine` (#EF9387), `cloud` (#AED5F4), `star` (#FFD43B), `berry` (#F17FAC), or `mint` (#73D9CA).
- `eyesId`: `dots`, `happy`, `sleepy`, `wink`, or `sunglasses`.
- `mouthId`: `smile`, `grin`, `open`, or `tongue`.
- `accessoryId`: `none`, `headphones`, `glasses`, `cap`, `bow`, or `flower`.

New presets follow the reference sheet's eight designs and ordering: orange circle with headphones, green square, lavender sunglasses triangle, blue cloud, pink star, yellow capped honeycomb, aqua diamond, and coral heart with headphones. Default shape-to-color IDs are `sunny`→`sunny`, `lime`→`lime`, `violet`→`violet`, `cloud`→`cloud`, `star`→`berry`, `tangerine`→`star`, `berry`→`mint`, and `mint`→`tangerine`. Their expressions and accessories are the preset defaults; color, eyes, mouth, and accessory remain independently editable.

Keep `Player.avatarId` as the legacy shape field. Add an optional serialized customization object to room players and join/create requests. When an older client omits it, derive the existing preset from `avatarId`; normalize defaults at the server boundary and reject unknown option IDs. Never accept arbitrary colors, image URLs, drawing instructions, or unbounded accessory data from clients.

## Rendering and client UI

- Render characters from layers: base shape, fill color, facial features, and accessory. Keep the dark outline and simple face style consistent with the existing characters.
- Android and web use the same option IDs and visual rules, with a live preview in the editor.
- Retain the eight-character reference grid as the quick path. Match its silhouettes, pastel fills, dark hand-drawn outlines, small faces, and thin legs/arms on both clients. Make the editor optional and compact on phones; use a wider panel on desktop.
- Update avatar displays in the lobby, player list, voting choices, reveal, and results. Use the legacy preset if a server snapshot has no customization object.
- Keep the generated transparent PNGs for decorative illustration or future use; the editable character itself should be drawn from configurable layers.

## State and transport

- Carry configuration with the room-create REST request for the host and the authenticated REST join request for every player.
- Store it on the server's `Player` model and include it in room snapshots so all clients render the same design.
- Keep the selected configuration locally between visits where the client's existing storage makes that straightforward. This phase does not promise cloud profile sync or cross-device restoration; the current player/account model does not store avatar profiles.
- Keep old clients and older room payloads working through nullable/default fields and the existing `avatarId` fallback.

## Implementation phases

1. **Contract and validation:** add the serializable configuration, curated catalogs, defaults, and server-side validation; preserve legacy `avatarId` behavior.
2. **Android:** add the optional editor and configurable Canvas renderer; include the config in create/join flows and all player-avatar displays.
3. **Web:** add matching controls and renderer; persist selection locally and include it in create/join flows and player-avatar displays.
4. **Integration review:** inspect all avatar call sites, compatibility fallbacks, labels/accessibility, and the changed files. Keep deployment and account-profile sync out of this slice.

## Completion criteria

- A player can keep a preset or customize shape, color, eyes, mouth, and accessory before joining.
- The live preview reflects every choice and survives navigating between create/join steps.
- Other players see the same avatar in lobby, voting, reveal, and results on Android and web.
- Missing customization data renders the original preset; invalid option IDs cannot create arbitrary server-side avatar state.
- Google linking remains optional and is not required for avatar creation or room play.
