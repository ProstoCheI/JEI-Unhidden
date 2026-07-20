# JEI Unhidden

**Every registered item, visible.**

JEI Unhidden is a lightweight client-side addon for
[Just Enough Items (JEI)](https://github.com/mezz/JustEnoughItems). It adds the
default stack of every item in Minecraft's item registry to JEI, including
items that other mods omit from creative tabs or remove from JEI's item list.

Depending on the installed mod version, this can expose internal, legacy, or
disabled content such as Refined Radiance and Shadow Steel from Create.

## Features

- Discovers items from every loaded mod through Minecraft's item registry.
- Adds items that are missing from creative-mode tabs.
- Restores registered items removed from JEI at runtime.
- Preserves the variants that JEI already knows about.
- Respects items manually hidden by the player with JEI's edit mode.
- Requires no configuration and adds no commands.
- Runs entirely on the client and is safe to include in modpacks.

## Requirements

| Component | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.234 or newer 21.1.x |
| JEI | 19.27.0.340 or newer 19.x |
| Java | 21 |

## Installation

1. Install NeoForge for Minecraft 1.21.1.
2. Install a compatible version of JEI.
3. Place the JEI Unhidden jar in the instance's `mods` directory.

The addon does not need to be installed on a dedicated server.

## What this mod does not do

- It does not add or enable recipes.
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
`build/libs/JEI Unhidden NeoForge v1.0.0 for mc1.21.1.jar`.

## License

JEI Unhidden is licensed under the
[GNU Lesser General Public License v3.0 only](LICENSE). The license file also
contains the incorporated GNU General Public License v3.0 terms.
