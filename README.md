# rhmr

A Fabric client mod that watches your active resource packs for file changes and automatically reloads them.

## Features

- **Auto-reload** - detects changes in active resource pack files/folders and triggers a reload automatically

## Requirements

- Minecraft 1.21.11
- [Fabric Loader](https://fabricmc.net/)
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [YACL](https://modrinth.com/mod/yacl)

## Configuration

Configurable via [ModMenu](https://modrinth.com/mod/modmenu) + [YACL](https://modrinth.com/mod/yacl).

| Option   | Default | Description                     |
|----------|---------|---------------------------------|
| Enabled  | `true`  | Enable/disable the mod entirely |
| Debounce | 0.5     | Debounce in seconds             |


Config is saved to `.minecraft/config/rhmr.json`.
