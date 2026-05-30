# rhmr

A Fabric client mod that watches your active resource packs for file changes and automatically reloads them. Pairs with [RRLS](https://modrinth.com/mod/rrls) to replace the reload screen with a minimal pixel animation.

## Features

- **Auto-reload** - detects changes in active resource pack files/folders and triggers a reload automatically
- **Reload indicator** - a small 8-frame spinning animation shown during any resource pack reload, replacing RRLS's default mini-render
- **"Reloaded!" label** - fades in next to the indicator when the reload finishes

## Requirements

- Minecraft 1.21.11
- [Fabric Loader](https://fabricmc.net/)
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [RRLS](https://modrinth.com/mod/rrls)
- [YACL](https://modrinth.com/mod/yacl)

## Configuration

Configurable via [ModMenu](https://modrinth.com/mod/modmenu) + [YACL](https://modrinth.com/mod/yacl).

| Option   | Default    | Description                               |
|----------|------------|-------------------------------------------|
| Enabled  | `true`     | Enable/disable the mod entirely           |
| Location | `Top left` | Corner where the reload indicator appears |
| Padding  | `3`        | Distance in pixels from the screen edge   |

Config is saved to `.minecraft/config/rhmr.json`.
