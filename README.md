# ChaosArcade

ChaosArcade is a Paper `1.21.11` mini-game plugin that combines multiple queue-based arcade modes into one server plugin with a shared GUI menu, arena system, and admin setup flow.

## Included Modes

Each mode is available from the shared `/arcade` menu and can also be joined directly with its command key.

| Mode | Command key | Players | Summary |
| --- | --- | --- | --- |
| `TNT_TAG` | `tnt_tag` | `2-12` | Pass the TNT before the timer explodes. |
| `BLOCK_SHUFFLE` | `block_shuffle` | `2-16` | Stand on the shown block or get eliminated. |
| `KING_OF_THE_HILL` | `king_of_the_hill` | `2-16` | Control the hill and score over time. |
| `INFECTION` | `infection` | `2-20` | Infected players convert the survivors. |
| `COLLAPSE_ARENA` | `collapse_arena` | `2-16` | Blocks disappear behind every step. |
| `MINING_RUSH` | `mining_rush` | `2-16` | Mine ores fast and stack points. |
| `ABILITY_BRAWL` | `ability_brawl` | `2-16` | Fight with shuffled special abilities. |

## Core Features

- Single `/arcade` entry point with GUI access and queue handling
- Seven playable mini-game modes in one plugin
- Arena creation, validation, and per-mode setup commands
- Shared lobby support
- Config-driven timings, scoring, and mode behavior
- Admin controls for reload, force start, and stopping active games

## Requirements

- Java `21`
- Maven `3.9+`
- Paper `1.21.11`

## Build

```powershell
mvn clean package
```

Output jar:

```text
target/chaos-arcade-1.0.0.jar
```

## Install

1. Build the jar with Maven.
2. Copy `target/chaos-arcade-1.0.0.jar` into your server `plugins/` folder.
3. Start the Paper server once.
4. Stop the server after the plugin generates its config if you want to review defaults.
5. Start the server again and finish setup in-game with the admin commands below.

Main config file:
- [src/main/resources/config.yml](src/main/resources/config.yml)

Plugin metadata:
- [src/main/resources/plugin.yml](src/main/resources/plugin.yml)

## Quick Setup

A mode will not open a queue until it has at least one fully configured arena.

Recommended first-time setup flow:

1. Join the server as an operator.
2. Stand in your lobby and run:
   - `/arcade setlobby`
3. Create your first arena for a mode:
   - `/arcade arena tnt_tag create arena1`
4. Set at least two spawn points for that arena:
   - `/arcade arena tnt_tag setspawn arena1 1`
   - `/arcade arena tnt_tag setspawn arena1 2`
5. Check whether the arena is ready:
   - `/arcade arena tnt_tag info arena1`
6. Open the menu with `/arcade` or join directly with:
   - `/arcade join tnt_tag`

If `/arcade join <mode>` says the mode has no ready arena yet, run `/arcade arena <mode> info <id>` and fix the missing setup fields.

## Arena Setup Commands

General admin commands:

- `/arcade setlobby`
  - Save the current location as the shared lobby.
- `/arcade reload`
  - Reload config and arena data.
- `/arcade forcestart`
  - Force-start the currently active queue if enough players are queued.
- `/arcade stop`
  - Stop the active queue or running match.

Arena management commands:

- `/arcade arena <mode> list`
- `/arcade arena <mode> create <id>`
- `/arcade arena <mode> delete <id>`
- `/arcade arena <mode> info <id>`
- `/arcade arena <mode> setspawn <id> <index>`
- `/arcade arena <mode> setcenter <id>`
- `/arcade arena <mode> setradius <id> <blocks>`
- `/arcade arena <mode> setpos1 <id>`
- `/arcade arena <mode> setpos2 <id>`

Player commands:

- `/arcade`
  - Open the main ChaosArcade GUI.
- `/arcade join <mode>`
  - Join a mode queue directly.
- `/arcade leave`
  - Leave the queue or current match.
- `/arcade queue`
  - Show queue or match status.

Aliases:

- `/chaosarcade`
- `/minigames`
- `/mg`

## Arena Requirements By Mode

All modes require at least `2` spawn points.

Extra requirements:

- `TNT_TAG`
  - Spawn points only.
- `BLOCK_SHUFFLE`
  - Spawn points only.
- `INFECTION`
  - Spawn points only.
- `ABILITY_BRAWL`
  - Spawn points only.
- `KING_OF_THE_HILL`
  - Spawn points, `center`, and `radius`.
- `COLLAPSE_ARENA`
  - Spawn points, `pos1`, and `pos2`.
- `MINING_RUSH`
  - Spawn points, `pos1`, and `pos2`.

Example setup for `KING_OF_THE_HILL`:

```text
/arcade arena king_of_the_hill create arena1
/arcade arena king_of_the_hill setspawn arena1 1
/arcade arena king_of_the_hill setspawn arena1 2
/arcade arena king_of_the_hill setcenter arena1
/arcade arena king_of_the_hill setradius arena1 8
/arcade arena king_of_the_hill info arena1
```

Example setup for `COLLAPSE_ARENA` or `MINING_RUSH`:

```text
/arcade arena collapse_arena create arena1
/arcade arena collapse_arena setspawn arena1 1
/arcade arena collapse_arena setspawn arena1 2
/arcade arena collapse_arena setpos1 arena1
/arcade arena collapse_arena setpos2 arena1
/arcade arena collapse_arena info arena1
```

## Permissions

- `chaosarcade.use`
  - Default: `true`
- `chaosarcade.admin`
  - Default: `op`

## Configuration

The config includes:

- shared plugin settings
- countdown timing
- per-mode duration or score targets
- block pool data for Block Shuffle
- lobby storage
- saved arenas per mode

## Project Structure

- `src/main/java/bg/nikol/chaosarcade`
  - Main plugin logic, commands, listeners, queue manager, arena model, and per-mode session classes
- `src/main/resources/plugin.yml`
  - Bukkit plugin metadata and permissions
- `src/main/resources/config.yml`
  - Default plugin configuration

## License

MIT
