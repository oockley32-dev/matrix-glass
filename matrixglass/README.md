# Matrix Glass (Fabric 1.21.1)

Digital rain, glass shards, bullet time, red/blue pills, a glowing code monolith and an exit payphone.

## Build
1. Install JDK 21. Run `gradle wrapper` once (or copy a wrapper from the Fabric example mod), then:
2. `./gradlew build`  ->  jar in `build/libs/`
3. `./gradlew runClient` to test in-game. Open the "Matrix Glass" creative tab.

## What's in it
- Red Pill: Matrix Vision (full-screen code rain, tint, glitch bars) for 2 min + Bullet Time for 20s
  (you get fast; mobs crawl; arrows hang in the air; shard afterimage trail).
- Blue Pill: clears all effects, short regen.
- Glass Monolith: 3D black-glass slab with animated falling-code panels and floating shards. Right-click for Matrix Vision (30s).
- Payphone: 3D model with EXIT sign. Right-click: it rings, then a 1x2 window opens in front of it showing your spawn (bed or world spawn). Step through to wake up there. The line drops after 15s. Needs Immersive Portals.
- Custom particles (code glyphs, tumbling glass shards), 5 synthesized sounds, recipes, loot tables.

## Regenerating assets
`pip install pillow numpy soundfile` then `python3 gen_assets.py` rewrites every texture, model and sound.
Models are plain JSON cubes: open them in Blockbench to edit.

## G.L.A.S.S. screens (needs Immersive Portals for 1.21.1 Fabric)
1. Set `immptl_version` in gradle.properties to the real Immersive Portals version number (see the comment there).
2. Place a **Matrix Terminal** where you want the camera. Right-click it to cycle its channel (0-7).
3. Build a solid rectangle of **Powered Glass** (wall, max 256 blocks) and put a **Matrix Projector** directly behind it, facing the glass.
4. Right-click the projector to toggle the screen. Sneak + right-click changes the projector's channel to match the terminal.
People view the glass from the side away from the projector. The view looks in the same compass direction as the projector faces.
All Immersive Portals calls live in `screen/PortalBridge.java`.
