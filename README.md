# JEI Unhidden

**Every registered item, visible.**

JEI Unhidden is a lightweight client-side addon for
[Just Enough Items (JEI)](https://github.com/mezz/JustEnoughItems). It adds the
default stack of every item in Minecraft's item registry to JEI, including
items that other mods omit from creative tabs or remove from JEI's item list.

Depending on the installed mod version, this can expose internal, legacy, or
disabled content.

## Features

- Discovers items from every loaded mod through Minecraft's item registry.
- Adds items that are missing from creative-mode tabs.
- Restores registered items removed from JEI at runtime.
- Preserves the variants that JEI already knows about.
- Respects items manually hidden by the player with JEI's edit mode.
- Provides per-mod visibility settings for items and recipes.
- Runs entirely on the client and is safe to include in modpacks.

## Requirements

| Component | Version |
| --- | --- |
| Minecraft | 1.21.7 or 1.21.8 |
| NeoForge for Minecraft 1.21.7 | 21.7.25-beta or newer 21.7.x |
| JEI for Minecraft 1.21.7 | 23.1.0.8 or newer 23.x |
| NeoForge for Minecraft 1.21.8 | 21.8.54 or newer 21.8.x |
| JEI for Minecraft 1.21.8 | 24.2.0.6 or newer 24.x |
| Java | 21 |

## Installation

1. Install NeoForge for Minecraft 1.21.7 or 1.21.8.
2. Install a compatible version of JEI.
3. Place the JEI Unhidden jar in the instance's `mods` directory.

The addon does not need to be installed on a dedicated server.

## What this mod does not do

- It does not register new recipes or change crafting behavior.
- It does not make unobtainable items obtainable in survival mode.
- It cannot show content that a mod never registered as an item.
- It does not generate every possible data-component or NBT combination.

Some exposed items are hidden by their authors for a reason and may be
unfinished, nonfunctional, or unsuitable for normal gameplay.

## Building from source

Clone the repository and run:

```powershell
.\gradlew.bat build
```

The compiled jar will be written to
`build/libs/JEI Unhidden NeoForge v1.0.1 for mc1.21.7-1.21.8.jar`.

The same jar supports both Minecraft versions. The project builds against
Minecraft 1.21.8 and JEI 24; Minecraft 1.21.7 uses JEI 23.

To build and preserve a copy for uploading, run:

```powershell
.\gradlew.bat saveRelease
```

The copy is saved in `releases/1.21.7-1.21.8/`. The `releases/` directory is ignored
by Git and persists when switching branches.
