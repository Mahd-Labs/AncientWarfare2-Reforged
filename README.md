# Ancient Warfare 2: Reforged

An unofficial port of the Minecraft mod Ancient Warfare 2 (2.7.0 for Minecraft 1.12.2, by Shadowmage, P3pp3rF1y and
contributors) to Minecraft 1.21.1 on NeoForge. It is not made or endorsed by the original authors. Status: 1.0.0,
the first release; keep backups of worlds you care about.

## Why this port

A friend of mine and I used this mod all the time back in the day and got up to all sorts of shenanigans. We always
talked about porting it some day, but never found the time. I decided to experiment with AI, and one of the
experiments turned into porting this mod for fun for a private modpack. I spent quite a bit more time than I thought I
would on this; had all decisions come to me before executing. As a result, there are a few improvements. NPCs should
react a little faster to changes, aiming should be a little more precise, etc. Each one can be switched off in the
[config](docs/CONFIG.md).

## In short

- **Requirements:** Minecraft 1.21.1 and NeoForge 21.1, the same jar on the server and every client.
- **Not ported from 1.12:** mobile siege weapons, boats, the battering ram, the chest cart, vehicle upgrades and armour.
- **Problems:** report them at https://github.com/Mahd-Labs/AncientWarfare2-Reforged/issues, not to the original authors.
- **Building:** with a Java 21 JDK, `./gradlew releaseJar` (the jar is in `build/release/`).
- **More:** settings in [docs/CONFIG.md](docs/CONFIG.md); known issues and 1.12 templates in
  [docs/DETAILS.md](docs/DETAILS.md); changes in [CHANGELOG.md](CHANGELOG.md).

## Licence and credits

GPL-3.0-or-later ([LICENSE.txt](LICENSE.txt)); what was changed and when: [NOTICE](NOTICE). Credits for the original
mod and its art, structures, skins, music and sounds: [CREDITS.md](CREDITS.md). The port's code was written largely
by AI agents (Claude, by Anthropic): see [AI_DISCLOSURE.md](AI_DISCLOSURE.md).
