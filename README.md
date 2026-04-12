# ChaosArcade

ChaosArcade is a Paper `1.21.11` mini-game plugin that bundles multiple queue-based arcade modes into one server plugin with a shared GUI menu, arena system, and admin setup flow.

## Included Modes

- `TNT_TAG`
- `BLOCK_SHUFFLE`
- `KING_OF_THE_HILL`
- `INFECTION`
- `COLLAPSE_ARENA`
- `MINING_RUSH`
- `ABILITY_BRAWL`

## Core Features

- Single `/arcade` entry point with GUI access
- Queue and match flow for multiple mini-game modes
- Arena creation and arena-specific setup commands
- Shared lobby support
- Config-driven game timing and mode settings
- Admin controls for reload, force start, and stop

## Commands

Player commands:

- `/arcade`
  - Open the main ChaosArcade GUI
- `/arcade join <mode>`
  - Join the queue for a mode
- `/arcade leave`
  - Leave the queue or the current match
- `/arcade queue`
  - Show queue or match status

Admin commands:

- `/arcade setlobby`
- `/arcade reload`
- `/arcade forcestart`
- `/arcade stop`
- `/arcade arena <mode> list`
- `/arcade arena <mode> create <id>`
- `/arcade arena <mode> delete <id>`
- `/arcade arena <mode> info <id>`
- `/arcade arena <mode> setspawn <id> <index>`
- `/arcade arena <mode> setcenter <id>`
- `/arcade arena <mode> setradius <id> <blocks>`
- `/arcade arena <mode> setpos1 <id>`
- `/arcade arena <mode> setpos2 <id>`

Aliases:

- `/chaosarcade`
- `/minigames`
- `/mg`

## Permissions

- `chaosarcade.use`
  - Default: `true`
- `chaosarcade.admin`
  - Default: `op`

## Arena Requirements

- `TNT_TAG`, `BLOCK_SHUFFLE`, `INFECTION`, `ABILITY_BRAWL`
  - Enough spawns for the mode
- `KING_OF_THE_HILL`
  - Spawns plus center plus radius
- `COLLAPSE_ARENA`, `MINING_RUSH`
  - Spawns plus `pos1` plus `pos2`

## Configuration

Main config file:

- [src/main/resources/config.yml](src/main/resources/config.yml)

Current config supports:

- shared plugin settings
- countdown timing
- per-mode round or duration values
- block pool for Block Shuffle
- kill target or points target depending on mode
- lobby location
- saved arenas

## Build

Requirements:

- Java `21`
- Maven

Build command:

```powershell
mvn clean package
```

The project uses:

- `paper-api:1.21.11-R0.1-SNAPSHOT`

Output jar:

```text
target/chaos-arcade-1.0.0.jar
```

## Install

1. Build the jar with Maven.
2. Copy `target/chaos-arcade-1.0.0.jar` into your Paper server `plugins/` folder.
3. Start the server once to generate config and data.
4. Configure the lobby and arenas with the admin commands.
5. Reload or restart the server after setup changes if needed.

## Project Structure

- `src/main/java/bg/nikol/chaosarcade`
  - Main plugin logic, commands, listeners, shared game manager, arena model, and per-mode session classes
- `src/main/resources/plugin.yml`
  - Bukkit plugin metadata
- `src/main/resources/config.yml`
  - Default plugin configuration

## Status

ChaosArcade is already a real working Paper plugin with multiple game modes and server-side setup flow. It is best presented as a mini-game hub plugin rather than a single-purpose command plugin.

## License

MIT
