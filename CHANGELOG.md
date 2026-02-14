# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.1] - 2026-02-14

### Fixed

- Fixed validation error in `remove_potion` and `remove_enchantment` actions.

## [1.2.0] - 2026-02-13

### Changed

- Removed logic for parsing legacy `filter` blocks.
    - IMPORTANT: This can affect legacy removal rules! Please remove any `filter` blocks from your JSONs as they are now
      redundant.

### Added

- Added config options for dev behaviour using EMI, like deleting items in-game by pressing the delete key.
- Added support for item tags in actions, using a `#` prefix or a `tags` block.
- Added `remove_trade` action to remove items only from Villager trades.
- Added `remove_loot` action to remove items only from loot tables.
- Added `remove_hand_swing` action to disable hand swinging.
- Added message when trying to attack or swing with items targeted by `remove_attack` or `remove_hand_swing` actions.

## [1.1.5] - 2026-02-12

### Added

- Removed items will now be automatically removed from containers when opened.
- Improved rule parsing performance.

## [1.1.4] - 2026-02-10

### Fixed

- Fixed crash with EMI loot.

## [1.1.3i] - 2026-02-09

### Fixed

- Bump Reliable Recipes version.

## [1.1.3h] - 2026-02-09

### Fixed

- Fixed issue with empty tags in recipe viewers.

## [1.1.3] - 2026-02-09

### Fixed

- Fixed incompatibility with Immersive Engineering.
- Fix log warning about missing EMI loot.

## [1.1.2] - 2026-02-04

### Changed

- Reliable Recipes is now bundled by default in the mod.

## [1.1.1] - 2026-01-30

### Added

- `/reliable_remover` command for dumping item ids
    - Supports `hotbar`, `inventory`, and `hand`

### Fixed

- Hopefully fixed issue with certain mods that could lead to loot tables being removed.

## [1.1.0] - 2026-01-30

### Added

- Support for arrays in Regex pattern matching.
- Much more lenient formatting for removal jsons.
    - Arrays and single strings should generally be interchangeable in actions and filters.
- Added dedicated Potion and Enchantment removal actions.
- Enchantment removals now also remove enchantments from Enchanting Tables.

### Fixed

- Fixed Regex patterns not re-applying on /reload.

### Changed

- Removed `filter` keyword for simplicity.
    - Existing patterns will still work, but it's good practice to remove the `filter` block.

## [1.0.13] - 2026-01-21

### Added

- Improved loot table removal to be more flexible with modded loot tables.

### Fixed

- Fix removed loot tables still showing up in EMI/JEI.

## [1.0.12] - 2026-01-12

### Fixed

- Fix "not" rule handling with dimension filtering.